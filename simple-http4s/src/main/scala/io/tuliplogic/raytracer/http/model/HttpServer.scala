package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.http.model.DrawingProgram.DrawEnv
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes}
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz._
import zio.random.Random
import zio.{RIO, ZIO}

class HttpServer[R <: DrawingProgram.DrawEnv with Clock with Random with DrawingRepository with Console](routes: DrawService[R]) {
  private val basePath = "/ray-tracer"

  type AppTask[A] = RIO[R, A]

  val router: HttpRoutes[AppTask] = Router[AppTask](basePath -> routes.httpRoutes)

  def httpApp: HttpApp[AppTask] = router.orNotFound

  val serve: ZIO[R, Throwable, Unit] = ZIO
    .runtime[R]
    .flatMap { implicit rts =>
      BlazeServerBuilder[AppTask]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
    }

}

object HttpServer {
  def make: HttpServer[DrawEnv with DrawingRepository with Clock with Console with Random] = new HttpServer(new DrawService[DrawEnv with DrawingRepository with Clock with Console with Random])
}
