package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.http.model.attapirato.DB.Transactor
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.BootstrapError
import io.tuliplogic.raytracer.http.model.attapirato.users.UsersRepo
import zio.blocking.Blocking
import zio.logging.{Logging, log}
import zio.logging.slf4j.Slf4jLogger
import zio.{App, ExitCode, URIO, ZIO, ZLayer}

object Main extends App {

  val slf4jLogger = Slf4jLogger.make((_, s) => s)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .catchAll {
        case e @ BootstrapError(_, _) => log.error(s"Error bootstrapping: ${e.message}; ${e.cause.foreach(_.printStackTrace())}")
        case other => log.error(s"Other error at startup, $other")
      }.provideCustomLayer((ZLayer.identity[Blocking] ++ slf4jLogger) >>> layer).exitCode
  }

  val transactorLayer = (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor
  val layer: ZLayer[Blocking with Logging, AppError with Product, Transactor with Logging with UsersRepo] =
    (transactorLayer ++ ZLayer.identity[Logging]) >+> UsersRepo.doobieLive

  val program: ZIO[UsersRepo with Transactor with Logging, BootstrapError, Unit] =
    for {
      _ <- log.info("Running Flyway migration...")
      _ <- DB.runFlyWay
      _ <- log.info("Flyway migration performed!")
      _ <- SimpleApp.serve.mapError(e => BootstrapError("Error starting http server", Some(e)))
    } yield ()

}
