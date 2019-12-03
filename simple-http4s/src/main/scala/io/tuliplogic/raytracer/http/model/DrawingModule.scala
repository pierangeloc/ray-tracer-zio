package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.http.model.Http2WorldModule.SceneBundle
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.programs.RaytracingProgram
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.ZIO

trait DrawingModule {
  val drawingModule: DrawingModule.Service[Any]
}

object DrawingModule { //content-type: image/png
  trait Service[R] {
    def draw(sceneBundle: SceneBundle): ZIO[R, Nothing, (String, Array[Byte])]
  }

  trait LivePng extends DrawingModule with RasteringModule with ATModule with CanvasSerializer { self =>
    val drawingModule: DrawingModule.Service[Any] = new Service[Any] {
      override def draw(sceneBundle: SceneBundle): ZIO[Any, Nothing, (String, Array[Byte])] =
        (for {
          canvas <- RaytracingProgram.drawOnCanvas(
            sceneBundle.world,
            sceneBundle.viewFrom,
            sceneBundle.viewTo,
            sceneBundle.viewUp,
            sceneBundle.visualAngleRad,
            sceneBundle.hRes, sceneBundle.vRes
          ).orDie
          bs <- CanvasSerializer.>.serializeAsByteStream(canvas, 255).runCollect
        } yield ("image/png", bs.toArray)).provide(self)
    }

  }
}
