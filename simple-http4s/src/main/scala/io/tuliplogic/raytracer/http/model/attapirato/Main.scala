package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.http.model.attapirato.DB.Transactor
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.BootstrapError
import io.tuliplogic.raytracer.http.model.attapirato.users.UsersRepo
import zio.blocking.Blocking
import zio.{App, ExitCode, URIO, ZIO, ZLayer}

object Main extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.provideCustomLayer(layer)
      .catchAll {
        case e @ BootstrapError(_, _, _) => zio.console.putStrLn(s"Error bootstrapping: ${e.message}; ${e.cause.foreach(_.printStackTrace())}")
        case other => zio.console.putStrLn(s"Other error at startup, $other")
      }.exitCode

  val layer: ZLayer[Blocking, AppError, UsersRepo with Transactor] =
    (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor >+> UsersRepo.doobieLive

  val program: ZIO[UsersRepo with Transactor, BootstrapError, Unit] =
    for {
      _ <- DB.runFlyWay
      _ <- SimpleApp.serve.mapError(e => BootstrapError(100, "Error starting http server", Some(e)))
    } yield ()

}
