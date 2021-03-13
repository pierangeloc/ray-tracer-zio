//package io.tuliplogic.raytracer.http.users
//
//import java.time.ZonedDateTime
//import java.util.UUID
//
//import eu.timepit.refined.auto._
//import io.tuliplogic.raytracer.http.model.attapirato.types.{AppError, user}
//import io.tuliplogic.raytracer.http.model.attapirato.types.user.{ClearPassword, Email, PasswordHash, User, UserId}
//import zio.{Has, IO, Ref, ULayer, URLayer, ZLayer}
//import zio.test.environment.TestEnvironment
//import zio.test._
//import zio.test.Assertion._
//import zio.test.{DefaultRunnableSpec, ZSpec, suite}
//import com.github.t3hnar.bcrypt._
//import eu.timepit.refined.types.string.NonEmptyString
//import io.tuliplogic.raytracer.http.types.AppError.DBError
//import io.tuliplogic.raytracer.http.types.user.{User, UserId}
//import zio.clock.Clock
//import zio.logging.Logging
//import zio.logging.slf4j.Slf4jLogger
//
//
//object UsersTest extends DefaultRunnableSpec {
//
//  val testUser = User(
//    UserId(UUID.randomUUID()),
//    Email("aeinstein@research.com"),
//    Some(PasswordHash(NonEmptyString.unsafeFrom("pwd123".boundedBcrypt))),
//    None,
//    None
//  )
//
//  val slf4jLogger: ULayer[Logging] = Slf4jLogger.make((_, s) => s)
//
//  val userRepo: URLayer[Has[Ref[Map[UserId, User]]], UsersRepo] = ZLayer.fromService (users =>
//    new UsersRepo.Service {
//      override def getUser(userId: UserId): IO[DBError, Option[User]] =
//        users.get.map(_.find(_._1 == userId).map(_._2))
//
//      override def getUserByEmail(email: Email): IO[AppError.DBError, Option[User]] =
//        users.get.map(_.find(_._2.email == email).map(_._2))
//
//
//      override def getUserByAccessToken(at: user.AccessToken): IO[AppError.DBError, Option[User]] =
//        users.get.map(_.find(_._2.accessToken.contains(at)).map(_._2))
//
//      override def createUser(user: User): IO[AppError.DBError, Unit] =
//        users.update(_ + (user.id -> user))
//      override def updatePassword(userId: UserId, newPassword: user.PasswordHash): IO[AppError.DBError, Unit] = {
//        users.update(_.updatedWith(userId){
//            case None => None
//            case Some(user) => Some(user.copy(password = Some(newPassword)))
//          }
//        )
//      }
//
//      override def updateAccessToken(userId: UserId, newAccessToken: user.AccessToken, expiresAt: ZonedDateTime): IO[AppError.DBError, Unit] =
//        users.update(_.updatedWith(userId){
//            case None => None
//            case Some(user) => Some(user.copy(accessToken = Some(newAccessToken), expiresAt = Some(System.currentTimeMillis() / 1000 + 86400)))
//          }
//        )
//    }
//  )
//
//  override def spec: ZSpec[TestEnvironment, Any] =
//    suite("Users.live")(
//      testM("Login successful when username and password match") {
//
//        val usersRepoLayer: ULayer[UsersRepo] = ZLayer.fromEffect(Ref.make(Map(testUser.id -> testUser))) >>> userRepo
//
//        (
//          for {
//            loginOutput <- Users.login(Email("aeinstein@research.com"), ClearPassword("pwd123"))
//          } yield assert(loginOutput.userId)(equalTo(testUser.id))
//        ).provideSomeLayer((slf4jLogger ++ usersRepoLayer ++ ZLayer.identity[Clock]) >>> Users.live)
//      }
//    )
//
//}
