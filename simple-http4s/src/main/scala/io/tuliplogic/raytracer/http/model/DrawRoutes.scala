package io.tuliplogic.raytracer.http.model

import org.http4s.{HttpRoutes, MediaType}
import org.http4s.dsl.Http4sDsl
import zio._
import io.tuliplogic.raytracer.commons.errors.IOError
import org.http4s.headers.`Content-Type`
import zio.interop.catz._
import io.circe._
import io.circe.generic.auto._
import io.tuliplogic.raytracer.commons.errors.IOError.HttpError
import io.tuliplogic.raytracer.http.model.DrawingRepoModel.DrawingState.{Done, Error, Started}
import io.tuliplogic.raytracer.http.model.DrawingRepoModel.{DrawingId, DrawingState}
import io.tuliplogic.raytracer.http.model.drawingRepository.DrawingRepository
import org.http4s._
import org.http4s.circe._
import zio.clock.Clock
import zio.console.Console
import zio.random.Random

class DrawRoutes[R <: DrawingProgram.DrawEnv with DrawingRepository with Random with Console with Clock] {
  type F[A] = RIO[R, A]
  private val http4sDsl = new Http4sDsl[F] {}
  import http4sDsl._

  implicitly[EntityEncoder[F, String]]

  val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "draw" =>
      implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]):
      EntityDecoder[F, A] = jsonOf[F, A]
      implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]):
      EntityEncoder[F, A] = jsonEncoderOf[F, A]
      req.decode[Scene] { scene =>
        val t: ZIO[DrawingProgram.DrawEnv with Console with Random with Clock with DrawingRepository, IOError.HttpError, DrawingId] = for {
          bundle    <- Http2World.httpScene2World(scene)
          startedAt <- zio.clock.nanoTime
          drawingId <- zio.random.nextLong.map(DrawingId.apply)
          _         <- drawingRepository.create(drawingId, startedAt / 1000).mapError(e => HttpError(e.toString))
          _         <- (DrawingProgram.draw(bundle).flatMap {
            case (contentType, bytes) => for {
              now       <- zio.clock.nanoTime
              _         <- drawingRepository.update(drawingId, DrawingState.Done(contentType, bytes, now - startedAt))
            } yield ()
          }).forkDaemon
          _         <- zio.console.putStrLn(s"Triggered computation for id: $drawingId")
        } yield drawingId

        t.foldM(
          e => zio.console.putStrLn(s"error ${e.getStackTrace.mkString("\n")}" + e) *> InternalServerError(s"something went wrong..., ${e.getStackTrace.mkString("\n")}"),
          drawingId => Ok(drawingId)
        )
      }

    case GET -> Root / "draw" =>
      implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[F, A] = jsonEncoderOf[F, A]
      drawingRepository.getAllIds.flatMap(Ok(_))

    case GET -> Root / "draw" / LongVar(id) =>
      for {
        drawingState <- drawingRepository.find(DrawingId(id))
        response     <- drawingState match {
          case e @ Error(_) =>
            implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]):
            EntityEncoder[F, A] = jsonEncoderOf[F, A]
            Ok(e)
          case s @ Started(_) =>
            implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]):
            EntityEncoder[F, A] = jsonEncoderOf[F, A]
            Ok(s)
          case Done(contentType, bytes, millisRequired) =>
            Ok(bytes, `Content-Type`(MediaType.unsafeParse(contentType)), Header("X-millis-required", millisRequired.toString))
        }
      } yield response

  }
}
