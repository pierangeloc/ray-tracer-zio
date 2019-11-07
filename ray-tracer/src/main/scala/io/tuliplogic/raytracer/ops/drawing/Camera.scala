package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import zio.{DefaultRuntime, UIO, ZIO}

/**
  * @param tf describes how the _world_ moves with respect to the camera
  */
case class Camera (
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

  override def toString: String = s"Camera(hRes = $hRes, vRes = $vRes, fieldOfView = $fieldOfViewRad)"
}

object Camera {

/**
  * Given
  * @param from The point where my eye is
  * @param to The point my eye is looking at
  * @param up The upper direction of my eye
  *
  * we provide the equivalent transformation of the world needed to produce the same perspective
  */
  def viewTransform(from: Pt, to: Pt, up: Vec): ZIO[ATModule, AlgebraicError, AT] = for {
      fwd             <- (to - from).normalized
      upNormalized  <- up.normalized
      left          <- UIO(fwd cross upNormalized)
      trueUp        <- UIO(left cross fwd) //this makes a real reference system LTR with fwd, up, left really orthogonal with each other
      orientationAT <- ATModule.>.invertible(
        left.x,   left.y,   left.z,   0d,
        trueUp.x, trueUp.y, trueUp.z, 0d,
        -fwd.x,   -fwd.y,   -fwd.z,   0d,
        0d,       0d,       0d,       1d
      )
      translateTf  <- ATModule.>.translate(-from.x, -from.y, -from.z)
      composed     <- ATModule.>.compose(translateTf,  orientationAT)
  } yield composed

  /**
    * Make a camera equipped with the transformation to be applied to the world to achieve the same effect
    * @param viewFrom the point where the camera is placed
    * @param viewTo the point the camera is looking to
    * @param upDirection the up vertical direction of the camera
    * @param visualAngleRad the visual angle in Rad of the camera
    * @param hRes horizontal resolution
    * @param vRes vertical resolution
    * @return
    */
  def make(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): ZIO[ATModule, AlgebraicError, Camera] = for {
    cameraTf <- viewTransform(viewFrom, viewTo, upDirection)
  } yield new Camera(hRes, vRes, visualAngleRad, cameraTf)

  def makeUnsafe(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): Camera =
    new DefaultRuntime{}.unsafeRun(
      Camera.make(viewFrom, viewTo, upDirection, visualAngleRad, hRes, vRes)
        .provide(new ATModule.Live with MatrixModule.BreezeMatrixModule)
    )

  //this is just the `identity` transformation
  val canonicalTransformation: ZIO[ATModule, AlgebraicError, AT] = viewTransform(Pt.origin, Pt(0, 0, -1), Vec.uy)

}
