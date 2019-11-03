package io.tuliplogic.raytracer.ops.programs

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.drawing.{Camera, World}
import io.tuliplogic.raytracer.ops.model.{Canvas, RasteringModule}
import zio.ZIO
import zio.stream.Sink

object RaytracingProgram {

  def drawOnCanvasWithCamera(world: World, camera: Camera, canvas: Canvas): ZIO[RasteringModule, RayTracerError, Unit] = for {
    coloredPointsStream <- RasteringModule.>.raster(world, camera)
    _ <- coloredPointsStream.mapM(cp => canvas.update(cp)).run(Sink.drain)
  } yield ()

  def drawOnCanvas(world: World, viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int):
    ZIO[RasteringModule with ATModule, RayTracerError, Canvas] = for {
    camera <- Camera.make(viewFrom, viewTo, upDirection, visualAngleRad, hRes, vRes)
    canvas <- Canvas.create(hRes, vRes)
    _      <- drawOnCanvasWithCamera(world, camera, canvas)
  } yield canvas

}