//package io.tuliplogic.raytracer.ops.programs
//
//import java.nio.file.{Path, Paths}
//
//import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
//import io.tuliplogic.raytracer.commons.errors.{CanvasError, RayTracerError}
//import io.tuliplogic.raytracer.geometry.affine.ATModule
//import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
//import io.tuliplogic.raytracer.ops.model.SceneObject.Sphere
//import io.tuliplogic.raytracer.ops.rendering.{CanvasSerializer, >}
//import io.tuliplogic.raytracer.ops.model.{Canvas, Color, Ray, RayModule}
//import zio.blocking.Blocking
//import zio.ZEnv
//import zio.clock.Clock
//import zio.console.Console
//import zio.{App, Chunk, UIO, ZIO, clock, console}
//
//import scala.{Stream => ScalaStream}
//import zio.stream._
//
//object Chapter5Sphere extends App {
//  val lightPt          = Pt(0, 0, -2.5)
//  val canvasHalfWidth  = 7d
//  val canvasHalfHeight = 7d
//  val canvasZCoord     = 10
//  val canvasHRes       = 500
//  val canvasVRes       = 500
//  val canvasFile       = "ppm/sphere-on-canvas.ppm"
//
//  def pixelsChunked(chunkSize: Int): ScalaStream[Chunk[(Pt, Int, Int)]] =
//    (for {
//      xn <- ScalaStream.from(0).take(canvasHRes)
//      yn <- ScalaStream.from(0).take(canvasVRes)
//    } yield (Pt(xn * (canvasHalfWidth * 2) / canvasHRes - canvasHalfWidth, yn * (canvasHalfHeight * 2) / canvasVRes - canvasHalfHeight, canvasZCoord), xn, yn))
//      .grouped(chunkSize)
//      .map(str => Chunk.fromIterable(str))
//      .toStream
//
//  def canvasPixelsAsPoints: StreamChunk[Nothing, (Pt, Int, Int)] =
//    StreamChunk(Stream.fromIterable(pixelsChunked(4096)))
//
//  def rayForPixel(px: Pt): Ray = Ray(origin = lightPt, direction = px - lightPt)
//
//  def intersectAndRender(px: Pt, sphere: Sphere, xn: Int, yn: Int, canvas: Canvas): ZIO[RayModule, CanvasError.IndexExceedCanvasDimension, Unit] =
//    for {
//      intersections <- RayModule.>.intersect(rayForPixel(px), sphere)
//      maybeHit      <- RayModule.>.hit(intersections)
//      _             <- maybeHit.fold(canvas.update(xn, yn, Color.black))(_ => canvas.update(xn, yn, Color.red))
//    } yield ()
//
//  val program: ZIO[CanvasSerializer with RayModule with ATModule with Console with Clock, RayTracerError, Unit] = for {
//    startTime <- clock.nanoTime
//    canvas    <- Canvas.create(canvasHRes, canvasVRes)
//    sphere    <- Sphere.unit
//    _ <- canvasPixelsAsPoints.foreach {
//      case (pt, xn, yn) =>
//        intersectAndRender(pt, sphere, xn, yn, canvas)
//    }
//    calcTime <- clock.nanoTime
//    _        <- console.putStr(s"computation time: ${(calcTime - startTime) / 1000} us")
//    _        <- >.render(canvas, 255)
//    endTime  <- clock.nanoTime
//    _        <- console.putStrLn(s"total time taken: ${(endTime - startTime) / 1000} us")
//  } yield ()
//
//  val program = for {
//    w      <- world
//      canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
//      _      <- CanvasSerializer.>.render(canvas, 255)
//  } yield ()
//
//  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
//    program
//      .provide {
//        new CanvasSerializer.PPMCanvasSerializer with RayModule.Live with ATModule.Live with MatrixModule.BreezeLive
//        with Clock.Live with Console.Live with Blocking.Live {
//          override def path: Path = Paths.get(canvasFile)
//        }
//      }
//      .foldM(err => console.putStrLn(s"Execution failed with: $err").as(1), _ => UIO.succeed(0))
//
//}
