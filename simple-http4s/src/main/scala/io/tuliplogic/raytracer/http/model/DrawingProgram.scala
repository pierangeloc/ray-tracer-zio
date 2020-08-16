package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.ops.model.data.World
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.programs.RaytracingProgram
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.ZIO
import zio.clock.Clock
import zio.console.Console

object DrawingProgram {
  type DrawEnv = CanvasSerializer with RasteringModule with ATModule

  case class SceneBundle(
                          world: World,
                          viewFrom: Pt,
                          viewTo: Pt,
                          viewUp: Vec,
                          visualAngleRad: Double,
                          hRes: Int,
                          vRes: Int
                        )

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
//    rows   <- canvas.rows
    bs     <- canvasSerializer.serializeAsByteStream(canvas, 255).runCollect
    _      <- zio.console.putStrLn(s"Png is baked, long ${bs.size} bytes")
  } yield ("image/png", bs.toArray)
}
