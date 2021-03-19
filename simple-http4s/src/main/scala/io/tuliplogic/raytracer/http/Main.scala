package io.tuliplogic.raytracer.http

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.matrix.matrixModule
import io.tuliplogic.raytracer.http.DB.Transactor
import io.tuliplogic.raytracer.http.drawings.{PngRenderer, Scenes, ScenesRepo}
import io.tuliplogic.raytracer.http.types.AppError.BootstrapError
import io.tuliplogic.raytracer.http.users.{Users, UsersRepo}
import io.tuliplogic.raytracer.ops.programs.layers
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import zio.clock.Clock
import zio.logging.{Logging, log}
import zio.logging.slf4j.Slf4jLogger
import zio.{App, ExitCode, URIO, ZIO}
import zio.magic._

object Main extends App {

  val slf4jLogger = Slf4jLogger.make((_, s) => s)

  /* //Giving a try to the debug feature
  val layer: ZLayer[zio.ZEnv, AppError, Users with Logging with Transactor with Scenes with Clock] =
    ZLayer.fromSomeMagicDebug[zio.ZEnv, Users with Logging with Transactor with Scenes with Clock](
      Users.live, slf4jLogger, DB.transactor, Scenes.live,
      Config.fromTypesafeConfig(),
      UsersRepo.doobieLive,
      ScenesRepo.doobieLive,
      PngRenderer.live,
      aTModule.live,
      canvasSerializer.pNGCanvasSerializer,
      layers.rasteringM,
      matrixModule.breezeLive
    )
    */

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .catchAll {
        case e @ BootstrapError(_, _) => log.error(s"Error bootstrapping: ${e.message}; ${e.cause.foreach(_.printStackTrace())}")
        case other => log.error(s"Other error at startup, $other")
      }.provideCustomMagicLayer(
      Users.live, slf4jLogger, DB.transactor, Scenes.live,
      Config.fromTypesafeConfig(),
      UsersRepo.doobieLive,
      ScenesRepo.doobieLive,
      PngRenderer.live,
      aTModule.live,
      canvasSerializer.pNGCanvasSerializer,
      layers.rasteringM,
      matrixModule.breezeLive
    ).exitCode
  }

  val program: ZIO[Users with Logging with Transactor with Scenes with Clock, BootstrapError, Unit] =
    for {
      _ <- log.info("Running Flyway migration...")
      _ <- DB.runFlyWay
      _ <- log.info("Flyway migration performed!")
      _ <- AllRoutes.serve.mapError(e => BootstrapError("Error starting http server", Some(e)))
    } yield ()

}
