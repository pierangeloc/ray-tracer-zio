package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.commons.errors.IOError.DrawingRepoError
import zio.macros.annotation.accessible
import zio.{Ref, ZIO}
import DrawingRepoModel._

@accessible(">")
trait DrawingRepository {
  val drawingRepository: DrawingRepository.Service[Any]
}

object DrawingRepository {
  trait Service[R] {
    def create(drawingId: DrawingId, started: Long): ZIO[R, DrawingRepoError, Unit]
    def update(drawingId: DrawingId, drawingState: DrawingState): ZIO[R, DrawingRepoError, Unit]
    def find(drawingId: DrawingId): ZIO[R, DrawingRepoError, DrawingState]
    def getAllIds: ZIO[R, Nothing, List[DrawingId]]
  }

  case class RefDrawingRepoService(ref: Ref[Map[DrawingId, DrawingState]]) extends Service[Any] {
    def create(drawingId: DrawingId, started: Long): ZIO[Any, DrawingRepoError, Unit] = for {
      _   <- ref.update(map => map + (drawingId -> DrawingState.Started(started)))
    } yield ()

    def update(drawingId: DrawingId, drawingState: DrawingState): ZIO[Any, DrawingRepoError, Unit] =
      ref.update(map => map.updated(drawingId, drawingState)).unit

    def find(drawingId: DrawingId): ZIO[Any, DrawingRepoError, DrawingState] =
      ref.get.flatMap { map =>
        ZIO.fromOption(map.get(drawingId))
      }.mapError(_ => DrawingRepoError(s"DrawingId $drawingId not fonud"))

    def getAllIds: ZIO[Any, Nothing, List[DrawingId]] =
      ref.get.map(_.keys.toList.sortWith((x, y) => x.value > y.value))
  }
}

object DrawingRepoModel {
  case class DrawingId(value: Long) extends AnyVal

  sealed trait DrawingState
  object DrawingState {
    case class Error(message: String) extends DrawingState
    case class Started(millisFromEpoch: Long) extends DrawingState
    case class Done(contentType: String, bytes: Array[Byte], millisRequired: Long) extends DrawingState
  }
}
