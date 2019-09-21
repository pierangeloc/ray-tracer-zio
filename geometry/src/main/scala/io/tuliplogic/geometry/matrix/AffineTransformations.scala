package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.Matrix.Col
import io.tuliplogic.raytracer.errors.MatrixError
import zio.{Chunk, UIO, ZIO}

//TODO: applyAffineTransformations require an implementation of matrix ops
object AffineTransformations {

  import math.{sin, cos}
  type Transformation = Matrix

  /**
   * this makes calculations simpler through matrix multiplication
   * - a point can be translated and moved (it's referred to the origin of the reference frame)
   * - a vector cannot be translated, i.e. if I translate a vector I get the same vector back, as a vector can be seen as always starting from the origin
   */

  def point(x: Double, y: Double, z: Double): UIO[Col]  = Matrix.createColVector(Chunk(x, y, z, 1))
  def vector(x: Double, y: Double, z: Double): UIO[Col] = Matrix.createColVector(Chunk(x, y, z, 0))

  def translation(x: Double, y: Double, z: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(1, 0, 0, x),
      Chunk(0, 1, 0, y),
      Chunk(0, 0, 1, z),
      Chunk(0, 0, 0, 1)
    )
  ).orDie

  def scaling(x: Double, y: Double, z: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(x, 0, 0, 0),
      Chunk(0, y, 0, 0),
      Chunk(0, 0, z, 0),
      Chunk(0, 0, 0, 1)
    )
  ).orDie

  def rotateX(θ: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(1, 0,      0,       0),
      Chunk(0, cos(θ), -sin(θ), 0),
      Chunk(0, sin(θ), cos(θ),  0),
      Chunk(0, 0,      0,       1)
    )
  ).orDie

  def rotateY(θ: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(cos(θ), 0, -sin(θ), 0),
      Chunk(0,      1, 0,       0),
      Chunk(sin(θ), 0, cos(θ),  0),
      Chunk(0,      0, 0,       1)
    )
  ).orDie

  def rotateZ(θ: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(cos(θ), -sin(θ), 0, 0),
      Chunk(sin(θ), cos(θ),  0, 0),
      Chunk(0,      0,       1, 0),
      Chunk(0,      0,       0, 1)
    )
  ).orDie

  def shearing(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): UIO[Matrix] =
    Matrix.fromRows(4, 4, Chunk(
      Chunk(1,  xY, xZ, 0),
      Chunk(yX, 1,  yZ, 0),
      Chunk(zX, zY, 1,  0),
      Chunk(0,  0,  0,  1)
    )
  ).orDie




}
