package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.ZIO

/**
  * @param tf describes how the _world_ moves with respect to the camera
  */
sealed abstract case class Camera private(
    hRes: Int,
    vRes: Int,
    fieldOfViewRad: Double,
    tf: AT
) {
  val aspectRatio: Double = hRes.toDouble / vRes.toDouble
  val halfView: Double    = math.tan(fieldOfViewRad / 2)

  val halfWidth: Double  = if (aspectRatio >= 1) halfView else halfView * aspectRatio
  val halfHeight: Double = if (aspectRatio >= 1) halfView / aspectRatio else halfView

  val pixelXSize: Double = 2 * halfWidth / hRes
  val pixelYSize: Double = pixelXSize //2 * halfHeight / vRes
}

object Camera {
  def make(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): ZIO[ATModule, AlgebraicError, Camera] = for {
    cameraTf <- ViewTransform(viewFrom, viewTo, upDirection).tf
  } yield new Camera(hRes, vRes, visualAngleRad, cameraTf){}
}
