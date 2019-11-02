package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.{MatrixModule, Types}
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.{UIO, ZIO}

/**
  * Given
  * @param from The point where my eye is
  * @param to The point my eye is looking at
  * @param up The upper direction of my eye
  *
  * we provide the equivalent transformation of the world needed to produce the same perspective
  * TODO: put all this in Camera
  */
case class ViewTransform(from: Pt, to: Pt, up: Vec) {

  def tf: ZIO[ATModule with MatrixModule, AlgebraicError, AT] = {
    for {
      fwd          <- (to - from).normalized
      upNormalized <- up.normalized
      left         <- UIO(fwd cross upNormalized)
      trueUp       <- UIO(left cross fwd) //this makes a real reference system LTR with fwd, up, left really orthogonal with each other
      orientationMatrix <- Types.factory.fromRows(
        4,
        4,
        Vector(
          Vector(left.x, left.y, left.z, 0d),
          Vector(trueUp.x, trueUp.y, trueUp.z, 0d),
          Vector(-fwd.x, -fwd.y, -fwd.z, 0d),
          Vector(0d, 0d, 0d, 1d)
        ))
      inverseMatrix <- MatrixModule.>.invert(orientationMatrix)
      translateTf <- ATModule.>.translate(-from.x, -from.y, -from.z)
      composed    <- ATModule.>.compose(translateTf,  AT(orientationMatrix, inverseMatrix))
    } yield composed
  }

}

object ViewTransform {
  val default = ViewTransform(Pt.origin, Pt(0, 0, -1), Vec.uy)
}
