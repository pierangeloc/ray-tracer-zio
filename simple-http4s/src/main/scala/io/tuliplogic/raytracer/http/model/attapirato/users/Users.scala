package io.tuliplogic.raytracer.http.model.attapirato.users

import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

import eu.timepit.refined.types.string.NonEmptyString
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, ClearPassword, Email, PasswordHash, User, UserId}
import zio.clock.Clock
import zio.logging.{Logger, Logging}
import zio.{IO, UIO, URLayer, ZIO, ZLayer}

object Users {

  trait Service {
    def createUser(email: Email): IO[APIError, UserCreated]
    def updatePassword(email: Email, newPassword: ClearPassword): IO[APIError, PasswordUpdated]
    def login(userEmail: Email, givenPassword: ClearPassword): IO[APIError, LoginSuccess]
    def authenticate(at: AccessToken): IO[APIError, UserId]
  }

  def createUser(email: Email): ZIO[Users, APIError, UserCreated] =
    ZIO.accessM(_.get.createUser(email))

  def updatePassword(email: Email, newPassword: ClearPassword): ZIO[Users, APIError, PasswordUpdated] =
    ZIO.accessM(_.get.updatePassword(email, newPassword))

  def login(userEmail: Email, clearPassword: ClearPassword): ZIO[Users, APIError, LoginSuccess] =
    ZIO.accessM(_.get.login(userEmail, clearPassword))

  def authenticate(at: AccessToken): ZIO[Users, APIError, UserId] =
    ZIO.accessM(_.get.authenticate(at))


  val live: URLayer[UsersRepo with Logging with Clock, Users] =
    ZLayer.fromServices[UsersRepo.Service, Logger[String], Clock.Service, Service] { (usersRepo, logger, clock) =>
    import com.github.t3hnar.bcrypt._

    new Service {

      def createUser(email: Email): IO[APIError, UserCreated] =
        UIO.effectTotal(UUID.randomUUID()).flatMap { id => {
          val userId = UserId(id)
          usersRepo.createUser(User(userId, email, None, None, None))
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
          user <- usersRepo.getUserByEmail(userEmail).catchAll(e =>
            logger.throwable("DB error fetching user by email", e) *>
              ZIO.fail(APIError("Couldn't fetch user"))
          ).some.mapError(_ => APIError("User not found"))
          pwdHash   <- user.password.fold[IO[APIError, PasswordHash]](ZIO.fail(APIError("Password not set for user, cannot authenticate")))(ZIO.succeed(_))
          newToken  <- createToken(clearPassword, pwdHash)
          now       <- clock.instant
          _ <- usersRepo.updateAccessToken(user.id, newToken, now.atZone(ZoneId.of("UTC")).plusSeconds(86400))
                .catchAll { dbErr =>
                  logger.throwable("DB Error updating access token", dbErr) *>
                    ZIO.fail(APIError("Could not update access token, you must login again"))
                }
        } yield LoginSuccess(user.id, newToken)

      def createToken(clearPassword: ClearPassword, userPwdHash: PasswordHash): IO[APIError, AccessToken] =
        if (clearPassword.value.value.isBcryptedBounded(userPwdHash.value.value)) AccessToken.generate(12) //TODO: try with fromServiceManyM
        else
          ZIO.fail(APIError("Wrong password"))

      def authenticate(at: AccessToken): IO[APIError, UserId] =
        for {
          maybeUser <- usersRepo.getUserByAccessToken(at)
                        .catchAll { dbErr =>
                          logger.throwable("DB Error getting user by access token", dbErr) *>
                            ZIO.fail(APIError("Could not perform lookup of user by access token"))
                      }
          nowSecs   <- clock.currentTime(TimeUnit.SECONDS)
          user      <- ZIO.fromOption(maybeUser).mapError(_ => APIError("Could not find a user for this access token"))
          _         <- ZIO.fromOption(user.expiresAt).mapError(_ => APIError("Could not find a user for this access token"))
                         .filterOrElse_(_ > nowSecs)(ZIO.fail(APIError("Access token expired, please login again")))
        } yield user.id
    }

  }

}
