package io.tuliplogic.raytracer.geometry.vectorspace

import io.tuliplogic.raytracer.geometry.matrix.Types.Col
import io.tuliplogic.raytracer.geometry.matrix.Types.factory
import io.tuliplogic.raytracer.geometry.matrix.Types.vectorizable.comp
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.commons.errors.AlgebraicError.IndexExceedMatrixDimension
import zio.{IO, UIO}

sealed trait PointVec
object PointVec {
  case class Pt(x: Double, y: Double, z: Double) extends PointVec {
    def -(otherPt: Pt): Vec = Vec(x - otherPt.x, y - otherPt.y, z - otherPt.z)
  }

  object Pt {
    val origin = Pt(0, 0, 0)
  }

  case class Vec(x: Double, y: Double, z: Double) extends PointVec {
    def plus(other: Vec): Vec   = Vec(x + other.x, y + other.y, z + other.z)
    def plus(otherPt: Pt): Pt   = Pt(x + otherPt.x, y + otherPt.y, z + otherPt.z)
    def minus(other: Vec): Vec  = Vec(x - other.x, y - other.y, z - other.z)
    def scale(t: Double)        = Vec(x * t, y * t, z * t)
    def dot(other: Vec): Double = x * other.x + y * other.y + z * other.z
    def cross(other: Vec): Vec = Vec(
      y * other.z - z * other.y,
      z * other.x - x * other.z,
      x * other.y - y * other.x
    )
    def normalized: IO[AlgebraicError, Vec] =
      for {
        l2   <- UIO.succeed(x * x + y * y + z * z)
        norm <- UIO.succeed(math.sqrt(l2))
        res  <- if (l2 == 0) IO.fail(AlgebraicError.VectorNonNormalizable) else IO.succeed(Vec(x / norm, y / norm, z / norm))
      } yield res

    def unary_- : Vec = Vec(-x, -y, -z)
  }

  object Vec {
    val zero = Vec(0, 0, 0)
    val ux   = Vec(1, 0, 0)
    val uy   = Vec(0, 1, 0)
    val uz   = Vec(0, 0, 1)
  }

  /**
    * this makes calculations simpler through matrix multiplication
    * - a point can be translated and moved (it's referred to the origin of the reference frame)
    * - a vector cannot be translated, i.e. if I translate a vector I get the same vector back, as a vector can be seen as always starting from the origin
    */
  def toCol(pt: Pt): UIO[factory.Col]   = factory.createColVector(comp(pt.x, pt.y, pt.z, 1))
  def toCol(vec: Vec): UIO[factory.Col] = factory.createColVector(comp(vec.x, vec.y, vec.z, 0))

  def colToPt(col: Col): IO[IndexExceedMatrixDimension, Pt] =
    for {
      x <- col.get(0, 0)
      y <- col.get(1, 0)
      z <- col.get(2, 0)
    } yield Pt(x, y, z)

  def colToVec(col: Col): IO[IndexExceedMatrixDimension, Vec] =
    for {
      x <- col.get(0, 0)
      y <- col.get(1, 0)
      z <- col.get(2, 0)
    } yield Vec(x, y, z)
}
