package io.tuliplogic.raytracer.http.drawings

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.http.DrawingProgram.SceneBundle
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.programs.RaytracingProgram
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.{Chunk, Has, UIO, URIO, URLayer, ZIO, ZLayer}

object PngRenderer {

  trait Service {
    def draw(scene: SceneBundle): UIO[Chunk[Byte]]
  }

  def draw(scene: SceneBundle): URIO[PngRenderer, Chunk[Byte]] =
    ZIO.accessM(_.get.draw(scene))

  val live: URLayer[CanvasSerializer with RasteringModule with ATModule, PngRenderer] =
    ZLayer.fromServices[canvasSerializer.Service, rasteringModule.Service, aTModule.Service, Service] {
      (serializer, rastering, at) =>
      new Service {
        override def draw(sceneBundle: SceneBundle): UIO[Chunk[Byte]] = for {

          canvas <- RaytracingProgram.drawOnCanvas(
            sceneBundle.world,
            sceneBundle.viewFrom,
            sceneBundle.viewTo,
            sceneBundle.viewUp,
            sceneBundle.visualAngleRad,
            sceneBundle.hRes, sceneBundle.vRes
          ).orDie.provideLayer(ZLayer.succeed(rastering) ++ ZLayer.succeed(at))
          bs     <- canvasSerializer.serializeAsByteStream(canvas, 255).runCollect.provide(Has(serializer))
        } yield bs
      }
    }
}
