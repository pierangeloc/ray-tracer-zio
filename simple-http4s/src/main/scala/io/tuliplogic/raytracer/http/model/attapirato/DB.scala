package io.tuliplogic.raytracer.http.model.attapirato

import cats.effect.Blocker
import zio.interop.catz._
import doobie.hikari.HikariTransactor
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.DBError
import zio.blocking.Blocking
import zio.{Runtime, Task, ZIO, ZManaged}

object DB {

  case class Config(
    url: String,
    user: String,
    password: String,
  )

  def transactor(config: Config): ZManaged[Blocking, DBError, HikariTransactor[Task]] =
    for {
      implicit0(rt: Runtime[Any]) <- ZIO.runtime[Any].toManaged_
      b                           <- ZIO.environment[Blocking].toManaged_
      blockingExecutor = b.get.blockingExecutor
      blockingEC       = blockingExecutor.asEC
      blocker          = Blocker.liftExecutionContext(blockingEC)
      transactor <-
        HikariTransactor.newHikariTransactor[Task](
          driverClassName = "org.postgresql.Driver",
          url = config.url,
          connectEC = blockingExecutor.asEC,
          blocker = blocker,
          user = config.user,
          pass = config.password
        ).toManagedZIO.mapError(t => DBError(100, "Error creating Hikari transactor", Some(t)))
    } yield transactor



}
