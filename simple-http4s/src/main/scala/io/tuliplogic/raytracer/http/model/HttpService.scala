package io.tuliplogic.raytracer.http.model

import org.http4s.{HttpRoutes, MediaType}
import org.http4s.dsl.Http4sDsl
import zio._
import io.circe.generic.auto._
import io.tuliplogic.raytracer.commons.errors.IOError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.interop.catz._
import org.http4s.headers.`Content-Type`


class HttpService[R] {
  type F[A] = RIO[R, A]
  private val http4sDsl = new Http4sDsl[F] {}
  import http4sDsl._

  val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "draw" =>
      req.decode[Scene] { scene =>
        val t: ZIO[CanvasSerializer with RasteringModule with ATModule, IOError.HttpError, (String, Array[Byte])] = for {
          bundle   <- Http2World.httpScene2World(scene)
          res <- DrawingProgram.draw(bundle)
        } yield res

        t.fold(
          _ => InternalServerError("something went wrong..."),
          {case (ct, bs) => Ok(bs, `Content-Type`(MediaType.unsafeParse(ct)))}
        )
      }
  }
}
