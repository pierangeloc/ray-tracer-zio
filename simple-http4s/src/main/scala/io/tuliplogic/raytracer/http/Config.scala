package io.tuliplogic.raytracer.http

import cats.effect.Blocker
import com.typesafe.config.ConfigFactory
import io.tuliplogic.raytracer.http.types.AppError.BootstrapError
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import pureconfig.generic.auto._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}

case class Config(
    db: Database
)

case class Database(
    url: String,
    user: String,
    password: String,
)

object Config {

  def fromTypesafeConfig(): ZLayer[Blocking, BootstrapError, Configuration] = (
    for {
      b <- ZIO.environment[Blocking]
      blockingExecutor = b.get.blockingExecutor
      blockingEC       = blockingExecutor.asEC
      blocker          = Blocker.liftExecutionContext(blockingEC)
      tsConfig <- Task.effect(ConfigFactory.load())
      cfg <- ConfigSource.fromConfig(tsConfig).loadF[Task, Config](blocker)
    } yield cfg
  ).mapError(e =>
    BootstrapError("Error reading configuration", Some(e))
  ).toLayer
}
