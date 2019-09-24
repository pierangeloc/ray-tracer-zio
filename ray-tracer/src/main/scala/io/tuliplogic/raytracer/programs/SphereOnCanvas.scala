package io.tuliplogic.raytracer.programs

import java.nio.file.{Path, Paths}

import cats.data.NonEmptyList
import io.tuliplogic.geometry.matrix.{AffineTransformationOps, MatrixOps}
import io.tuliplogic.geometry.matrix.SpatialEntity.Pt
import io.tuliplogic.geometry.matrix.SpatialEntity.SceneObject.Sphere
import io.tuliplogic.raytracer.errors.{CanvasError, RayTracerError}
import io.tuliplogic.raytracer.io.rendering.{canvasRendering, CanvasRenderer}
import io.tuliplogic.raytracer.model.{rayOps, Canvas, Color, Ray, RayOperations}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{clock, console, App, UIO, ZIO}

import scala.{Stream => ScalaStream}
import zio.stream._

object SphereOnCanvas extends App {
  val lightPt          = Pt(0, 0, -5)
  val canvasHalfWidth  = 7d
  val canvasHalfHeight = 7d
  val canvasZCoord     = 10
  val canvasHRes       = 500
  val canvasVRes       = 500
  val canvasFile       = "/tmp/nioexp/sphere-on-canvas.ppm"

  def canvasPixelsAsPoints: Stream[Nothing, (Pt, Int, Int)] =
    for {
      xn <- Stream.fromIterable(ScalaStream.from(0)).take(canvasHRes)
      yn <- Stream.fromIterable(ScalaStream.from(0)).take(canvasVRes)
    } yield (Pt(xn * (canvasHalfWidth * 2) / canvasHRes - canvasHalfWidth, yn * (canvasHalfHeight * 2) / canvasVRes - canvasHalfHeight, canvasZCoord), xn, yn)

  def rayForPixel(px: Pt): Ray = Ray(origin = lightPt, direction = px - lightPt)

  def intersectAndRender(px: Pt, sphere: Sphere, xn: Int, yn: Int, canvas: Canvas): ZIO[RayOperations, CanvasError.IndexExceedCanvasDimension, Unit] =
    for {
      intersections <- rayOps.intersect(rayForPixel(px), sphere)
      maybeHit      <- NonEmptyList.fromList(intersections).map(ix => rayOps.hit(ix).map(Some(_))).getOrElse(UIO(None))
      _             <- maybeHit.fold(canvas.update(xn, yn, Color.black))(_ => canvas.update(xn, yn, Color.red))
    } yield ()

  val program: ZIO[CanvasRenderer with RayOperations with Clock with Console, RayTracerError, Unit] = for {
    startTime <- clock.nanoTime
    canvas    <- Canvas.create(canvasHRes, canvasVRes)
    sphere    <- Sphere.unit
    _ <- canvasPixelsAsPoints.foreach {
      case (pt, xn, yn) =>
        intersectAndRender(pt, sphere, xn, yn, canvas)
    }
    _       <- canvasRendering.render(canvas, 255)
    endTime <- clock.nanoTime
    _       <- console.putStrLn(s"time taken: ${(endTime - startTime) / 1000} us")
  } yield ()

  override def run(args: List[String]): ZIO[SphereOnCanvas.Environment, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer with RayOperations.Live with Blocking.Live with MatrixOps.Live with Console.Live with Clock.Live
        with AffineTransformationOps.Live {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: $err").as(1), _ => UIO.succeed(0))

}
