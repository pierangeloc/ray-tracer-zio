package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.http.model.attapirato.DB.Transactor
import io.tuliplogic.raytracer.http.model.attapirato.drawings.{PngRenderer, Scenes, ScenesRepo}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.BootstrapError
import io.tuliplogic.raytracer.http.model.attapirato.users.{Users, UsersRepo}
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
      }.provideCustomLayer((ZLayer.identity[Blocking with Clock] ++ slf4jLogger) >>> layer).exitCode
  }

  val transactorLayer: ZLayer[Blocking, AppError, Transactor] = (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor
  val usersLayer: URLayer[Transactor with Logging with Clock, Users] =
    (UsersRepo.doobieLive ++ ZLayer.identity[Clock] ++ ZLayer.identity[Logging]) >>> Users.live

  val l1: ZLayer[Blocking with Logging with Transactor, Nothing, ATModule with RasteringModule with CanvasSerializer with PngRenderer with Logging with ScenesRepo] = (((layers.atM >+> layers.rasteringM) ++ layers.cSerializerM) >+> PngRenderer.live) ++ ZLayer.identity[Logging] ++ ScenesRepo.doobieLive
  val scenesLayer: ZLayer[Blocking with Logging with Transactor, Nothing, Scenes] =
     l1 >>>
      Scenes.live

  val baseLayer: ZLayer[Blocking with Logging with Clock, AppError, Transactor with Logging with Blocking with Clock] =
    transactorLayer ++ ZLayer.identity[Logging] ++ ZLayer.identity[Clock] ++ ZLayer.identity[Blocking]

  val layer: ZLayer[Blocking with Logging with Clock, AppError, Transactor with Logging with Blocking with Clock with Users with Scenes] =
    baseLayer >+> (usersLayer ++ scenesLayer)

  val program: ZIO[Users with Logging with Transactor with Scenes, BootstrapError, Unit] =
    for {
      _ <- log.info("Running Flyway migration...")
      _ <- DB.runFlyWay
      _ <- log.info("Flyway migration performed!")
      _ <- AllRoutes.serve.mapError(e => BootstrapError("Error starting http server", Some(e)))
    } yield ()

}
