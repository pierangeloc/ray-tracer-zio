package io.tuliplogic.raytracer.http.model.attapirato.users

import java.time.ZoneId
import java.util.UUID

import eu.timepit.refined.types.string.NonEmptyString
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, ClearPassword, Email, PasswordHash, User, UserId}
import zio.clock.Clock
import zio.logging.{Logger, Logging}
import zio.{Has, IO, UIO, URLayer, ZIO, ZLayer}

object Users {

  trait Service {
    def createUser(email: Email): IO[APIError, UserCreated]
    def updatePassword(email: Email, newPassword: ClearPassword): IO[APIError, PasswordUpdated]
    def login(userEmail: Email, givenPassword: ClearPassword): IO[APIError, LoginSuccess]
  }

  def createUser(email: Email): ZIO[Users, APIError, UserCreated] =
    ZIO.accessM(_.get.createUser(email))

  def updatePassword(email: Email, newPassword: ClearPassword): ZIO[Users, APIError, PasswordUpdated] =
    ZIO.accessM(_.get.updatePassword(email, newPassword))

  def login(userEmail: Email, clearPassword: ClearPassword): ZIO[Users, APIError, LoginSuccess] =
  ZIO.accessM(_.get.login(userEmail, clearPassword))

  val live: URLayer[UsersRepo with Logging with Clock, Has[Service]] =
    ZLayer.fromServices[UsersRepo.Service, Logger[String], Clock.Service, Service] { (usersRepo, logger, clock) =>
    import com.github.t3hnar.bcrypt._

    new Service {

      def createUser(email: Email): IO[APIError, UserCreated] =
        UIO.effectTotal(UUID.randomUUID()).flatMap { id => {
          val userId = UserId(id)
          usersRepo.createUser(User(userId, email, None, None))
            .foldM(e =>
              logger.throwable(s"Error creating user", e) *> ZIO.fail(APIError("Error creating user")),
              _ => ZIO.succeed(UserCreated(userId)))
        }
      }

      def updatePassword(email: Email, newPassword: ClearPassword): IO[APIError, PasswordUpdated] =
        for {
          user   <- usersRepo.getUserByEmail(email).collect(APIError("User not found")){
            case Some(u) => u
          }.catchAll(dbError => logger.throwable("Error updating password", dbError) *> ZIO.fail(APIError("Error updating password")))
          hashed <- ZIO.effectTotal(newPassword.value.value.boundedBcrypt)
          _ <- usersRepo.updatePassword(user.id, PasswordHash(NonEmptyString.unsafeFrom(hashed)))
                .catchAll(dbError => logger.throwable("Error updating password", dbError) *> ZIO.fail(APIError("Error updating password")))
        } yield PasswordUpdated(user.id)

      def login(userEmail: Email, clearPassword: ClearPassword): IO[APIError, LoginSuccess] =
        for {
          maybeUser <- usersRepo.getUserByEmail(userEmail).catchAll(e =>
            logger.throwable("DB error fetching user by email", e) *> ZIO.fail(APIError("Couldn't fetch user"))
          )
          user      <- maybeUser.fold[IO[APIError, User]](ZIO.fail(APIError("User not found")))(u => ZIO.succeed(u))
          pwdHash   <- user.password.fold[IO[APIError, PasswordHash]](ZIO.fail(APIError("Password not set for user, cannot authenticate")))(ZIO.succeed(_))
          newToken  <- createToken(clearPassword, pwdHash)
          now       <- clock.instant
          _ <- usersRepo.updateAccessToken(user.id, newToken, now.atZone(ZoneId.of("UTC")))
                .catchAll {
                   dbErr => logger.throwable("DB Error updating access token", dbErr).as(APIError("Could not update access token, you must login again"))
                }
        } yield LoginSuccess(user.id, newToken)

      def createToken(clearPassword: ClearPassword, userPwdHash: PasswordHash): IO[APIError, AccessToken] =
        if (clearPassword.value.value.isBcryptedBounded(userPwdHash.value.value)) AccessToken.generate(12) //TODO: try with fromServiceManyM
        else
          ZIO.fail(APIError("Wrong password"))

    }

  }

}
