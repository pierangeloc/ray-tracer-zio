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
import io.tuliplogic.raytracer.commons.errors.IOError.HttpError
import io.tuliplogic.raytracer.http.model.DrawingRepoModel.DrawingState.{Done, Error, Started}
import io.tuliplogic.raytracer.http.model.DrawingRepoModel.{DrawingId, DrawingState}
import org.http4s._
import org.http4s.circe._
import zio.clock.Clock
import zio.console.Console
import zio.random.Random

class DrawService[R <: DrawingProgram.DrawEnv with DrawingRepository with Random with Console with Clock] {
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
        val t: ZIO[DrawingProgram.DrawEnv with Console with Random with Clock with DrawingRepository, IOError.HttpError, DrawingId] = for {
          bundle    <- Http2World.httpScene2World(scene)
          startedAt <- zio.clock.nanoTime
          drawingId <- zio.random.nextLong.map(DrawingId.apply)
          _         <- DrawingRepository.>.create(drawingId, startedAt / 1000).mapError(e => HttpError(e.toString))
          _         <- (DrawingProgram.draw(bundle).flatMap {
            case (contenType, bytes) => for {
              now       <- zio.clock.nanoTime
              _         <- DrawingRepository.>.update(drawingId, DrawingState.Done(contenType, bytes, now - startedAt))
            } yield ()
          }).fork
          _         <- zio.console.putStrLn(s"Triggered computation for id: $drawingId")
        } yield drawingId

        t.foldM(
          e => zio.console.putStrLn(s"error ${e.getStackTrace.mkString("\n")}" + e) *> InternalServerError(s"something went wrong..., ${e.getStackTrace.mkString("\n")}"),
          drawingId => Ok(drawingId)
        )
      }

    case req @ GET -> Root / "draw" / LongVar(id) =>
      for {
        drawingState <- DrawingRepository.>.find(DrawingId(id))
        response     <- drawingState match {
          case e @ Error(_) => Ok(e)
          case s @ Started(_) => Ok(s)
          case Done(contentType, bytes, millisRequired) =>
            Ok(bytes, `Content-Type`(MediaType.unsafeParse(contentType)), Header("X-millis-required", millisRequired.toString))
        }
      } yield response

  }
}
