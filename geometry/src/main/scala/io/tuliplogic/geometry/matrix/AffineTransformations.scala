package io.tuliplogic.geometry.matrix

import io.tuliplogic.raytracer.errors.MatrixError
import zio.{Chunk, UIO, ZIO}

//TODO: applyAffineTransformations require an implementation of matrix ops
object AffineTransformations {
  import Types._

  import vectorizable.comp

  import math.{sin, cos}
  type Transformation = M

  /**
   * this makes calculations simpler through matrix multiplication
   * - a point can be translated and moved (it's referred to the origin of the reference frame)
   * - a vector cannot be translated, i.e. if I translate a vector I get the same vector back, as a vector can be seen as always starting from the origin
   */

  def point(x: Double, y: Double, z: Double): UIO[Col]  = factory.createColVector(comp(x, y, z, 1))
  def vector(x: Double, y: Double, z: Double): UIO[Col] = factory.createColVector(comp(x, y, z, 0))

  def translate(x: Double, y: Double, z: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(1d, 0d, 0d, x),
      comp(0d, 1d, 0d, y),
      comp(0d, 0d, 1d, z),
      comp(0d, 0d, 0d, 1d)
    )
  ).orDie

  def scale(x: Double, y: Double, z: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(x, 0d, 0d, 0d),
      comp(0d, y, 0d, 0d),
      comp(0d, 0d, z, 0d),
      comp(0d, 0d, 0d, 1d)
    )
  ).orDie

  def rotateX(θ: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(1d, 0d,      0d,       0d),
      comp(0d, cos(θ), -sin(θ), 0d),
      comp(0d, sin(θ), cos(θ),  0d),
      comp(0d, 0d,      0d,       1d)
    )
  ).orDie

  def rotateY(θ: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(cos(θ), 0d, -sin(θ), 0d),
      comp(0d,      1d, 0d,       0d),
      comp(sin(θ), 0d, cos(θ),  0d),
      comp(0d,      0d, 0d,       1d)
    )
  ).orDie

  def rotateZ(θ: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(cos(θ), -sin(θ), 0d, 0d),
      comp(sin(θ), cos(θ),  0d, 0d),
      comp(0d,      0d,       1d, 0d),
      comp(0d,      0d,       0d, 1d)
    )
  ).orDie

  def shearing(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): UIO[M] =
    factory.fromRows(4, 4, comp(
      comp(1d,  xY, xZ, 0d),
      comp(yX, 1d,  yZ, 0d),
      comp(zX, zY, 1d,  0d),
      comp(0d,  0d,  0d,  1d)
    )
  ).orDie

}
