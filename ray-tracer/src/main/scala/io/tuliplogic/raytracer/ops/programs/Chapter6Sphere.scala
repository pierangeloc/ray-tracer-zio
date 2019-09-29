package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import cats.data.NonEmptyList
import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, BusinessError, CanvasError, RayTracerError}
import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{Canvas, Color, Intersection, Material, PhongReflection, Ray, RayOperations, SpatialEntityOperations, phongOps, rayOps, spatialEntityOps}
import io.tuliplogic.raytracer.ops.rendering.{CanvasRenderer, canvasRendering}
import spire.math.Algebraic
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.stream._
import zio.{App, Chunk, IO, UIO, ZIO, clock, console}

import scala.{Stream => ScalaStream}

object Chapter6Sphere extends App {
  val sphereMaterial   = Material.default.copy(color = Color(1, 0.2, 1))
  val infinitePoint    = Pt(0, 0, -2.5)
  val pointLight          = PointLight(Pt(-10, 10, -10), Color.white)
  val canvasHalfWidth  = 7d
  val canvasHalfHeight = 7d
  val canvasZCoord     = 10
  val canvasHRes       = 500
  val canvasVRes       = 500
  val canvasFile       = "/tmp/nioexp/chapter-6-shaded-sphere.ppm"

  def pixelsChunked(chunkSize: Int): ScalaStream[Chunk[(Pt, Int, Int)]] = (for {
    xn <- ScalaStream.from(0).take(canvasHRes)
    yn <- ScalaStream.from(0).take(canvasVRes)
  } yield (Pt(xn * (canvasHalfWidth * 2) / canvasHRes - canvasHalfWidth, yn * (canvasHalfHeight * 2) / canvasVRes - canvasHalfHeight, canvasZCoord), xn, yn)
    ).grouped(chunkSize).map(str => Chunk.fromIterable(str)).toStream

  def canvasPixelsAsPoints: StreamChunk[Nothing, (Pt, Int, Int)] =
    StreamChunk(Stream.fromIterable(pixelsChunked(4096)))

  def rayForPixel(px: Pt): IO[AlgebraicError, Ray] =
    for {
      normalizedDirection <- (px - infinitePoint).normalized
    } yield Ray(origin = infinitePoint, direction = normalizedDirection)

  def colorForHit(ray: Ray, hit: Intersection): ZIO[PhongReflection with SpatialEntityOperations with RayOperations, BusinessError.GenericError, PhongReflection.PhongComponents] = hit.sceneObject match {
    case s@Sphere(_, _) =>
      for {
        pt <- rayOps.positionAt(ray, hit.t)
        normalV <- spatialEntityOps.normal(pt, s)
        eyeV    <- UIO(-ray.direction)
        color   <- phongOps.lighting(sphereMaterial, pointLight, pt, eyeV, normalV)
      } yield color
    case _ => IO.fail(BusinessError.GenericError("can't handle anything but spheres"))
  }

  type RichRayOperations = PhongReflection with SpatialEntityOperations with RayOperations
  object RichRayOperations {
    trait Live extends PhongReflection.Live with SpatialEntityOperations.Live with RayOperations.Live
  }
  def intersectAndRender(px: Pt, sphere: Sphere, xn: Int, yn: Int, canvas: Canvas) =
    for {
      ray           <- rayForPixel(px)
      intersections <- rayOps.intersect(ray, sphere)
      maybeHit      <- NonEmptyList.fromList(intersections).map(ix => rayOps.hit(ix).map(Some(_))).getOrElse(UIO(None))
      _             <- maybeHit.fold[ZIO[RichRayOperations, RayTracerError, Unit]](
                         canvas.update(xn, yn, Color.black)
                       ) { hit =>
                         colorForHit(ray, hit).flatMap(phongComps => canvas.update(xn, yn, phongComps.toColor))
                       }
    } yield ()

  val program: ZIO[CanvasRenderer with RichRayOperations with Clock with Console, RayTracerError, Unit] = for {
    startTime <- clock.nanoTime
    canvas    <- Canvas.create(canvasHRes, canvasVRes)
    sphere    <- Sphere.unit
    _ <- canvasPixelsAsPoints.foreach {
      case (pt, xn, yn) =>
        intersectAndRender(pt, sphere, xn, yn, canvas)
    }
    calcTime <- clock.nanoTime
    _       <-  console.putStr(s"computation time: ${(calcTime - startTime) / 1000} us")
    _       <- canvasRendering.render(canvas, 255)
    endTime <- clock.nanoTime
    _       <- console.putStrLn(s"total time taken: ${(endTime - startTime) / 1000} us")
  } yield ()

  override def run(args: List[String]): ZIO[Chapter5Sphere.Environment, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer with RichRayOperations.Live with Blocking.Live with MatrixOps.Live with Console.Live with Clock.Live
        with AffineTransformationOps.Live {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: $err").as(1), _ => UIO.succeed(0))

}
