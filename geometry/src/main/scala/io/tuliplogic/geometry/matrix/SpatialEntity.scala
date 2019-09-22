package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.Types.{Col, factory}
import io.tuliplogic.raytracer.errors.MatrixError.IndexExceedMatrixDimension
import zio.{IO, UIO}

sealed trait SpatialEntity
  import io.tuliplogic.geometry.matrix.Types.vectorizable.comp


object SpatialEntity {
  case class Pt(x: Double, y: Double, z: Double)  extends SpatialEntity
  case class Vec(x: Double, y: Double, z: Double) extends SpatialEntity

  /**
   * this makes calculations simpler through matrix multiplication
   * - a point can be translated and moved (it's referred to the origin of the reference frame)
   * - a vector cannot be translated, i.e. if I translate a vector I get the same vector back, as a vector can be seen as always starting from the origin
   */

  def toCol(pt: Pt):   UIO[factory.Col] = factory.createColVector(comp(pt.x, pt.y, pt.z, 1))
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



