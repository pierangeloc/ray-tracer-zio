package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.affine.AT

/**
  * @param tf describes how the _world_ moves with respect to the camera
  */
case class Camera(
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
