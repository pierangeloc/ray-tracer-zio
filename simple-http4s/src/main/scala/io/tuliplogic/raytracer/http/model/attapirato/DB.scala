package io.tuliplogic.raytracer.http.model.attapirato

import cats.effect.Blocker
import zio.interop.catz._
import doobie.hikari.HikariTransactor
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.{BootstrapError, DBError}
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.{Has, Runtime, Task, ZIO, ZLayer}

object DB {

  type Transactor = Has[HikariTransactor[Task]]

  def transactor: ZLayer[Blocking with Configuration, DBError, Transactor] =
    (for {
      implicit0(rt: Runtime[Any]) <- ZIO.runtime[Any].toManaged_
      b                           <- ZIO.environment[Blocking].toManaged_
      config                      <- ZIO.service[Config].toManaged_
      blockingExecutor = b.get.blockingExecutor
      blockingEC       = blockingExecutor.asEC
      blocker          = Blocker.liftExecutionContext(blockingEC)

      transactor <-
        HikariTransactor.newHikariTransactor[Task](
          driverClassName = "org.postgresql.Driver",
          url = config.db.url,
          connectEC = blockingExecutor.asEC,
          blocker = blocker,
          user = config.db.user,
          pass = config.db.password
        ).toManagedZIO.mapError(t => DBError("Error creating Hikari transactor", Some(t)))
    } yield transactor).toLayer

  def runFlyWay: ZIO[Transactor, BootstrapError, Int] =
    ZIO.service[HikariTransactor[Task]].flatMap { transactor =>
      transactor.configure { ds =>
        for {
          flyway <- Task(Flyway.configure().dataSource(ds).load())
          _      <- Task(flyway.baseline())
          res    <- Task(flyway.migrate())
        } yield res
      }.mapError(t => BootstrapError("Error running flyway", Some(t)))
    }

}
