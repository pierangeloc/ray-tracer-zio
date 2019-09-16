package io.tuliplogic.geometry.matrix

import io.tuliplogic.raytracer.errors.MatrixError
import zio.ZIO

//matrix m rows by n cols
class Matrix(private val m: Int, n: Int, rows: List[List[Double]]) {
  val cols = rows.transpose

  def transpose: Matrix = new Matrix(n, m, cols)
}

object Matrix {
  /*
    some util methods to:
      - create a matrix from elements
      - create a matrix of zeroes
      - create a matrix of ones
      - create a diagonal matrix
      - create identity matrix
      - multiply matrix by scalar

      type aliases for vectors
   */

}

trait MatrixOps {
  def matrixOps: MatrixOps.Service[Any]
}

object MatrixOps {

  /*
    - add
    - mul
    - invert
   */
  trait Service[R] {
    def add(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def mul(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def invert(m: Matrix): ZIO[R, MatrixError, Matrix]
  }
}