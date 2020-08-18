package io.tuliplogic.raytracer.http

import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.http.DB.Transactor
import io.tuliplogic.raytracer.http.drawings.{PngRenderer, Scenes, ScenesRepo}
import io.tuliplogic.raytracer.http.types.AppError
import io.tuliplogic.raytracer.http.types.AppError.BootstrapError
import io.tuliplogic.raytracer.http.users.{Users, UsersRepo}
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.programs.layers
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.{Logging, log}
import zio.logging.slf4j.Slf4jLogger
import zio.{App, ExitCode, URIO, URLayer, ZIO, ZLayer}

object Main extends App {

  val slf4jLogger = Slf4jLogger.make((_, s) => s)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .catchAll {
        case e @ BootstrapError(_, _) => log.error(s"Error bootstrapping: ${e.message}; ${e.cause.foreach(_.printStackTrace())}")
        case other => log.error(s"Other error at startup, $other")
      }.provideCustomLayer((ZLayer.identity[Blocking with Clock] ++ slf4jLogger) >>> Layers.programLayer).exitCode
  }

  val program: ZIO[Users with Logging with Transactor with Scenes, BootstrapError, Unit] =
    for {
      _ <- log.info("Running Flyway migration...")
      _ <- DB.runFlyWay
      _ <- log.info("Flyway migration performed!")
      _ <- AllRoutes.serve.mapError(e => BootstrapError("Error starting http server", Some(e)))
    } yield ()

}

object Layers {
  type AppEnv = Blocking with Clock with Logging
  val baseLayer = ZLayer.identity[AppEnv]

  val transactorLayer: ZLayer[Blocking, AppError, Transactor] =
    (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor

  val withTransactor: ZLayer[AppEnv, AppError, Transactor with AppEnv] =
    transactorLayer ++ baseLayer

  val usersLayer: ZLayer[Transactor with AppEnv, AppError, Users] =
    (UsersRepo.doobieLive ++ baseLayer) >>> Users.live

  val pngRendererLayer: URLayer[Blocking, ATModule with RasteringModule with CanvasSerializer with PngRenderer] =
    ((layers.atM >+> layers.rasteringM) ++ layers.cSerializerM) >+> PngRenderer.live

  val scenesLayer: ZLayer[Transactor with Blocking with Logging, Nothing, Scenes] =
    (ScenesRepo.doobieLive ++ pngRendererLayer ++ ZLayer.identity[Logging]) >>> Scenes.live

  val programLayer: ZLayer[AppEnv, AppError, Users with Scenes with Transactor with Logging] =
    (transactorLayer ++ baseLayer) >+> (usersLayer ++ scenesLayer)

  val fullLayer: ZLayer[AppEnv, AppError, Users] =
    (transactorLayer ++ baseLayer) >>> usersLayer


}
