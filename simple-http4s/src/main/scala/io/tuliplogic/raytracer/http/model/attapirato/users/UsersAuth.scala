package io.tuliplogic.raytracer.http.model.attapirato.users

import java.time.ZoneId

import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, Email, PasswordHash, User}
import zio.clock.Clock
import zio.logging.{Logger, Logging}
import zio.random.Random
import zio.{Has, IO, URLayer, ZIO, ZLayer}

object UsersAuth {

  trait Service {
    def login(userEmail: Email, givenPassword: String): IO[APIError, User]
  }

  val live: URLayer[UsersRepo with Logging with Random with Clock, Has[Service]] =
    ZLayer.fromServices[UsersRepo.Service, Logger[String],  Random.Service, Clock.Service, Service] { (repoService, logger, randomService, clock) =>
    import com.github.t3hnar.bcrypt._


    new Service {
      def login(userEmail: Email, givenPassword: String): IO[APIError, User] =
        for {
          maybeUser <- repoService.getUserByEmail(userEmail).catchAll(e =>
            logger.throwable("DB error fetching user by email", e) *> ZIO.fail(APIError("Couldn't fetch user")))
          user      <- maybeUser.fold[IO[APIError, User]](ZIO.fail(APIError("User not found")))(u => ZIO.succeed(u))
          pwdHash   <- user.password.fold[IO[APIError, PasswordHash]](ZIO.fail(APIError("Password not set for user, cannot authenticate")))(ZIO.succeed(_))
          newToken  <- createToken(givenPassword, pwdHash)
          now       <- clock.instant
          _ <- repoService.updateAccessToken(user.id, newToken, now.atZone(ZoneId.of("UTC")))
                .orElseFail(APIError("Could not update access token, you must login again"))
        } yield ???

      def createToken(givenPassword: String, userPwdHash: PasswordHash): IO[APIError, AccessToken] =
        if (givenPassword.isBcryptedBounded(userPwdHash.value.value)) AccessToken.generate(12).provide(Has(randomService)) //TODO: try with fromServiceManyM
        else
          ZIO.fail(APIError("Wrong password"))

    }

  }

}
