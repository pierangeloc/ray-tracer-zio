package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.http.model.Http2World.SceneBundle
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.programs.RaytracingProgram
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.ZIO
import zio.clock.Clock
import zio.console.Console

object DrawingProgram {
  type DrawEnv = CanvasSerializer with RasteringModule with ATModule

  def draw(sceneBundle: SceneBundle): ZIO[CanvasSerializer with RasteringModule with ATModule with Console with Clock, Nothing, (String, Array[Byte])] = for {
    _      <- zio.console.putStrLn("Created scene bundle, now drawing the world...")
    canvas <- RaytracingProgram.drawOnCanvas(
      sceneBundle.world,
      sceneBundle.viewFrom,
      sceneBundle.viewTo,
      sceneBundle.viewUp,
      sceneBundle.visualAngleRad,
      sceneBundle.hRes, sceneBundle.vRes
      ).orDie
    _      <- zio.console.putStrLn("World is drawn, baking png...")
    rows   <- canvas.rows
    _      <- ZIO.effectTotal(println(s"The canvas is: ${rows.map(_.toList.take(10)).toList.take(10)}"))
    bs     <- canvasSerializer.serializeAsByteStream(canvas, 255).runCollect
    _      <- zio.console.putStrLn(s"Png is baked, long ${bs.size} bytes")
  } yield ("image/png", bs.toArray)
}
