package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.http.model.Http2World.SceneBundle
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.programs.RaytracingProgram
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.ZIO

object DrawingProgram {
  def draw(sceneBundle: SceneBundle): ZIO[CanvasSerializer with RasteringModule with ATModule, Nothing, (String, Array[Byte])] = for {
    canvas <- RaytracingProgram.drawOnCanvas(
      sceneBundle.world,
      sceneBundle.viewFrom,
      sceneBundle.viewTo,
      sceneBundle.viewUp,
      sceneBundle.visualAngleRad,
      sceneBundle.hRes, sceneBundle.vRes
      ).orDie
    bs <- CanvasSerializer.>.serializeAsByteStream(canvas, 255).runCollect
  } yield ("image/png", bs.toArray)
}
