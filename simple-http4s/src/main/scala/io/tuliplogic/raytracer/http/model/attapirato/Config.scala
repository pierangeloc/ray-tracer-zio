package io.tuliplogic.raytracer.http.model.attapirato
import com.typesafe.config.ConfigFactory
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.BootstrapError
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import pureconfig.generic.auto._
import zio.interop.catz._
import zio.{Has, Layer, Task}

case class Config(
    db: Database
)

case class Database(
    url: String,
    user: String,
    password: String,
)

object Config {

  def fromTypesafeConfig(): Layer[BootstrapError, Has[Config]] = (
    for {
      tsConfig <- Task.effect(ConfigFactory.load())
      cfg <- ConfigSource.fromConfig(tsConfig).loadF[Task, Config]
    } yield cfg
  ).mapError(e =>
    BootstrapError(300, "Error reading configuration", Some(e))
  ).toLayer
}
