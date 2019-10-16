package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.geometry.vectorspace.{affineTfOps, AffineTransformation, AffineTransformationOps}
import io.tuliplogic.raytracer.ops.model.{Canvas, Ray}
import zio.{UIO, ZIO}

/**
  * @param tf describes how the _world_ moves with respect to the camera
  */
case class Camera(
    hRes: Int,
    vRes: Int,
    fieldOfViewRad: Double,
    tf: AffineTransformation
) {
  val aspectRatio: Double = hRes.toDouble / vRes.toDouble
  val halfView: Double    = math.tan(fieldOfViewRad / 2)

  val halfWidth: Double  = if (aspectRatio >= 1) halfView else halfView * aspectRatio
  val halfHeight: Double = if (aspectRatio >= 1) halfView / aspectRatio else halfView

  val pixelXSize: Double = 2 * halfWidth / hRes
  val pixelYSize: Double = pixelXSize //2 * halfHeight / vRes

  def rayForPixel(px: Int, py: Int): ZIO[AffineTransformationOps, AlgebraicError, Ray] =
    for {
      xOffset <- UIO((px + 0.5) * pixelXSize)
      yOffset <- UIO((py + 0.5) * pixelYSize)
      //coordinates of the canvas point before the transformation
      origX <- UIO(halfWidth - xOffset)
      origY <- UIO(halfHeight - yOffset)
      //transform the coordinates by the inverse
      inverseTf <- affineTfOps.invert(tf)
      pixel     <- affineTfOps.transform(inverseTf, Pt(origX, origY, -1))
      origin    <- affineTfOps.transform(inverseTf, Pt.origin)
      direction <- (pixel - origin).normalized
    } yield Ray(origin, direction)

}
