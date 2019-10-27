package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.affine.{ATModule, AffineTransformationOps, affineTfOps}
import io.tuliplogic.raytracer.ops.drawing.Camera
import zio.{UIO, ZIO}


trait CameraModule {
  val cameraModule: CameraModule.Service[Any]
}

object CameraModule {

  trait Service[R] {
    def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[R, AlgebraicError, Ray]
  }

  trait Live extends CameraModule {
    val aTModule: ATModule.Service[Any]

    val cameraModule: CameraModule.Service[Any] = new Service[Any] {
      override def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[Any, AlgebraicError, Ray] =
        for {
          xOffset <- UIO((px + 0.5) * camera.pixelXSize)
            yOffset <- UIO((py + 0.5) * camera.pixelYSize)
            //coordinates of the canvas point before the transformation
            origX <- UIO(camera.halfWidth - xOffset)
            origY <- UIO(camera.halfHeight - yOffset)
            //transform the coordinates by the inverse
            inverseTf <- aTModule.invert(camera.tf)
            pixel     <- aTModule.applyTf(inverseTf, Pt(origX, origY, -1))
            origin    <- aTModule.applyTf(inverseTf, Pt.origin)
            direction <- (pixel - origin).normalized
        } yield Ray(origin, direction)
    }
  }

  object > extends CameraModule.Service[CameraModule] {
    override def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[CameraModule, AlgebraicError, Ray] =
      ZIO.accessM(_.cameraModule.rayForPixel(camera, px, py))
  }
}
