package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.commons.errors.IOError.DrawingRepoError
import zio.{Has, IO, Ref, UIO, URIO, ZIO, ZLayer}
import DrawingRepoModel._
import zio.ZLayer.NoDeps

object drawingRepository {

  trait Service {
    def create(drawingId: DrawingId, started: Long): IO[DrawingRepoError, Unit]
    def update(drawingId: DrawingId, drawingState: DrawingState): IO[DrawingRepoError, Unit]
    def find(drawingId: DrawingId): IO[DrawingRepoError, DrawingState]
    def getAllIds: UIO[List[DrawingId]]
  }

  type DrawingRepository = Has[Service]

  def refDrawingRepoService(ref: Ref[Map[DrawingId, DrawingState]]): NoDeps[Nothing, Has[Service]] = ZLayer.succeed(new Service {
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
  })

  def create(drawingId: DrawingId, started: Long): ZIO[DrawingRepository, DrawingRepoError, Unit] =
    ZIO.accessM(_.get.create(drawingId, started))
  def update(drawingId: DrawingId, drawingState: DrawingState): ZIO[DrawingRepository, DrawingRepoError, Unit] =
    ZIO.accessM(_.get.update(drawingId, drawingState))
  def find(drawingId: DrawingId): ZIO[DrawingRepository, DrawingRepoError, DrawingState] =
    ZIO.accessM(_.get.find(drawingId))
  def getAllIds: URIO[DrawingRepository, List[DrawingId]] =
    ZIO.accessM(_.get.getAllIds)

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
