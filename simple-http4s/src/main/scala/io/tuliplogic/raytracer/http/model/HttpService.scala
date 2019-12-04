package io.tuliplogic.raytracer.http.model

import org.http4s.{HttpRoutes, MediaType}
import org.http4s.dsl.Http4sDsl
import zio._
import io.tuliplogic.raytracer.commons.errors.IOError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import org.http4s.headers.`Content-Type`
import zio.interop.catz._
import io.circe._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._

class HttpService[R <: CanvasSerializer with RasteringModule with ATModule] {
  type F[A] = RIO[R, A]
  private val http4sDsl = new Http4sDsl[F] {}
  import http4sDsl._

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]):
  EntityDecoder[F, A] = jsonOf[F, A]
  implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]):
  EntityEncoder[F, A] = jsonEncoderOf[F, A]

  val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "draw" =>
      req.decode[Scene] { scene =>
        val t: ZIO[CanvasSerializer with RasteringModule with ATModule, IOError.HttpError, (String, Array[Byte])] = for {
          bundle  <- Http2World.httpScene2World(scene)
          res     <- DrawingProgram.draw(bundle)
        } yield res

        t.foldM(
          _ => InternalServerError("something went wrong..."),
          {case (ct, bs) => Ok(bs, `Content-Type`(MediaType.unsafeParse(ct)))}
        )
      }
  }
}
