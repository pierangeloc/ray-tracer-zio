package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.ops.model.data.{Camera, Ray}
import zio.{Has, IO, UIO, ZIO, ZLayer}

object cameraModule {

  trait Service {
    def rayForPixel(camera: Camera, px: Int, py: Int): IO[Nothing, Ray]
  }

  type CameraModule = Has[Service]

  val live: ZLayer[ATModule, Nothing, CameraModule] = ZLayer.fromService { atSvc =>
    new Service {
      // Implementation: the canonical camera has the eye in Pt.origin, and the screen on the plane z = -1,
      // therefore after computing the coordinates of the point in the screen, we have to apply the _inverse of the camera transformation_
      // because the camera transformation is the transformation to be applied to the world in order to produce the effect of moving/orienting the camera around
      // This transformation must be applied both to the point in the camera, and to the origin. Then the computation of the ray is trivial.
      override def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[Any, Nothing, Ray] =
        for {
          xOffset   <- UIO((px + 0.5) * camera.pixelXSize)
          yOffset   <- UIO((py + 0.5) * camera.pixelYSize)
          //coordinates of the canvas point before the transformation
          origX     <- UIO(camera.halfWidth - xOffset)
          origY     <- UIO(camera.halfHeight - yOffset)
          //transform the coordinates by the inverse
          inverseTf <- atSvc.invert(camera.tf)
          pixel     <- atSvc.applyTf(inverseTf, Pt(origX, origY, -1))
          origin    <- atSvc.applyTf(inverseTf, Pt.origin)
          direction <- (pixel - origin).normalized.orDie
        } yield Ray(origin, direction)
    }
  }

  def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[CameraModule, Nothing, Ray] =
    ZIO.accessM(_.get.rayForPixel(camera, px, py))
}
