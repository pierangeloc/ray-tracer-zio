package io.tuliplogic.raytracer.http.model.attapirato.users

import java.time.ZonedDateTime

import doobie._
import doobie.hikari.HikariTransactor
import io.tuliplogic.raytracer.http.model.attapirato.DB
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.DBError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, Email, PasswordHash, User, UserId}
import zio.{IO, Task, URLayer, ZIO, ZLayer}

object UsersRepo {

  trait Service {
    def getUser(userId: UserId): IO[DBError, Option[User]]
    def getUserByEmail(email: Email): IO[DBError, Option[User]]
    def createUser(user: User): IO[DBError, Unit]
    def updatePassword(userId: UserId, newPassword: PasswordHash): IO[DBError, Unit]
    def updateAccessToken(userId: UserId, newAccessToken: AccessToken, expiresAt: ZonedDateTime): IO[DBError, Unit]
  }

  // accessor methods
  def getUser(userId: UserId): ZIO[UsersRepo, DBError, Option[User]] =
    ZIO.accessM(_.get.getUser(userId))

  def getUserByEmail(email: Email): ZIO[UsersRepo, DBError, Option[User]] =
    ZIO.accessM(_.get.getUserByEmail(email))

  def createUser(user: User): ZIO[UsersRepo, DBError, Unit] =
    ZIO.accessM(_.get.createUser(user))

  def updatePassword(userId: UserId, newPassword: PasswordHash): ZIO[UsersRepo, DBError, Unit] =
    ZIO.accessM(_.get.updatePassword(userId, newPassword))

  val doobieLive: URLayer[DB.Transactor, UsersRepo] =
    ZLayer.fromService[HikariTransactor[Task], UsersRepo.Service] { transactor =>
      new Service {

        import doobie.implicits._
        import zio.interop.catz._

        override def getUser(userId: UserId): IO[DBError, Option[User]] = {
          Queries.getUser(userId)
            .option.transact(transactor)
            .mapError(e => DBError(s"Error fetching user with id = $userId", Some(e)))
        }


        override def getUserByEmail(email: Email): IO[DBError, Option[User]] =
          Queries.getUserByEmail(email)
            .option.transact(transactor)
            .mapError(e => DBError(s"Error fetching user with email = $email", Some(e)))

        override def createUser(user: User): IO[DBError, Unit] = {
          Queries.createUser(user)
            .run.transact(transactor)
            .mapError(e => DBError(s"Error creating user with id = ${user.id}", Some(e)))
            .unit
        }


        override def updatePassword(userId: UserId, newPassword: PasswordHash): IO[DBError, Unit] =
          Queries.updatePassword(userId, newPassword)
            .run.transact(transactor)
            .mapError(e => DBError(s"Error updating password for user with id = $userId", Some(e)))
            .unit

        override def updateAccessToken(userId: UserId, newAccessToken: AccessToken, expiresAt: ZonedDateTime): IO[DBError, Unit] =
          Queries.updateAccessToken(userId, newAccessToken, expiresAt)
            .run.transact(transactor)
            .mapError(e => DBError(s"Error updating access token for user with id = $userId", Some(e)))
            .unit

      }
    }

  object Queries {


    import doobie.implicits._
    import doobie.refined.implicits._
    import doobie.postgres.implicits._
    import io.tuliplogic.raytracer.http.model.attapirato.doobieUtils._

    def getUser(userId: UserId): Query0[User] =
      sql"""select * from users
           |  where id = ${userId.value}
           """.stripMargin.query[User]

    def getUserByEmail(email: Email): Query0[User] =
      sql"""select * from users
           |  where email = ${email.value}
         """.stripMargin.query[User]

    def createUser(user: User): Update0 =
      sql"""
           |insert into users (
           | "id", "email", "password_hash"
           |) VALUES (
           | ${user.id.value},
           | ${user.email.value},
           | ${user.password.fold("NULL")(_.value.value)}
           |)
           |""".stripMargin.update

    def updatePassword(userId: UserId, newPassword: PasswordHash): Update0 =
      sql"""
           |update users
           |  set "password_hash" = ${newPassword.value.value}
           |  where id = ${userId.value}
           |""".stripMargin.update

    def updateAccessToken(userId: UserId, accessToken: AccessToken, expiresAt: ZonedDateTime): Update0 =
      sql"""
           |update users
           |  set "access_token"            = ${accessToken.value.value},
           |      "access_token_expires_at" = ${expiresAt.toEpochSecond}
           |  where id = ${userId.value}
           |""".stripMargin.update
  }

}
