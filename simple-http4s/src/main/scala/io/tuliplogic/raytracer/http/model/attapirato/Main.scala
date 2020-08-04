package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.http.model.attapirato.DB.Transactor
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError
import zio.blocking.Blocking
import zio.{App, ExitCode, Task, URIO, ZIO, ZLayer}

object Main extends App {

  def fail(b: Boolean): Task[Unit] = if (b) Task.fail(new Exception()) else Task.succeed(())

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.exitCode

  val layer: ZLayer[Blocking, AppError, Transactor] =
    (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor

  val program: ZIO[Blocking, AppError, Int] = DB.runFlyWay.provideLayer(layer)

}
