package io.tuliplogic.raytracer.http.model.attapirato.users

//import java.util.UUID

import io.tuliplogic.raytracer.http.model.attapirato.{DB}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.DBError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{Email, PasswordHash, User, UserId}
import zio.{IO, URLayer, ZLayer}

object UsersRepo {

  trait Service {
    def getUser(userId: UserId): IO[DBError, Option[User]]
    def getUserByEmail(email: Email): IO[DBError, Option[User]]
    def createUser(user: User): IO[DBError, Unit]
    def updatePassword(userId: UserId, newPassword: PasswordHash): IO[DBError, Unit]
  }

  val doobieLive: URLayer[DB.Transactor, UsersRepo] =
    ZLayer.fromService { transactor =>
      new Service {

        import doobie.implicits._
        import doobie.refined.implicits._
        import zio.interop.catz._
        import doobie.postgres.implicits._
        import io.tuliplogic.raytracer.http.model.attapirato.doobieUtils._

        override def getUser(userId: UserId): IO[DBError, Option[User]] =
          sql"""select * from users
               |  where id = ${userId.value}
               """
            .query[User].option.transact(transactor)
            .mapError(e => DBError(200, s"Error fetching user with id = $userId", Some(e)))


        override def getUserByEmail(email: Email): IO[DBError, Option[User]] =
          sql"""select * from users
               |  where email = ${email.value}
               """
            .query[User].option.transact(transactor)
            .mapError(e => DBError(200, s"Error fetching user with email = $email", Some(e)))

        override def createUser(user: User): IO[DBError, Unit] =
          sql"""
            |insert into users (
            | "id", "email", "password"
            |) VALUES (
            | ${user.id.value},
            | ${user.email.value},
            | ${user.password.fold("NULL")(_.value.value)}
            |)
            |""".update.run.transact(transactor)
            .mapError(e => DBError(200, s"Error creating user with id = ${user.id}", Some(e)))
            .unit


        override def updatePassword(userId: UserId, newPassword: PasswordHash): IO[DBError, Unit] =
          sql"""
               |update users
               |  set "password" = ${newPassword.value.value}
               |  where id = ${userId.value}
               |""".update.run.transact(transactor)
            .mapError(e => DBError(200, s"Error updating password for user with id = $userId", Some(e)))
            .unit

      }
    }

}
