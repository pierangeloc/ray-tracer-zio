package io.tuliplogic.geometry.matrix

import io.tuliplogic.raytracer.errors.CanvasError.IndexExceedCanvasDimension
import io.tuliplogic.raytracer.errors.MatrixError
import io.tuliplogic.raytracer.errors.MatrixError.{IndexExceedMatrixDimension, MatrixConstructionError, MatrixDimError}
import zio.{IO, _}
import mouse.all._

//matrix m rows by n cols
class Matrix private (private val m_ : Int, n_ : Int, rows_ : Chunk[Chunk[Double]]) {
  private val cols_ : Chunk[Chunk[Double]] = rows_.toArray.map(_.toArray).transpose.map(Chunk.fromArray) |> Chunk.fromArray

  def m: UIO[Int]                     = UIO.succeed(m_)
  def n: UIO[Int]                     = UIO.succeed(n_)
  def rows: UIO[Chunk[Chunk[Double]]] = UIO.succeed(rows_)
  def cols: UIO[Chunk[Chunk[Double]]] = UIO.succeed(cols_)
  def transpose: UIO[Matrix]          = UIO.succeed(new Matrix(n_, m_, cols_))
  def get(i: Int, j: Int): IO[IndexExceedMatrixDimension, Double] =
    Matrix.checkAccessIndex(i, j, m_, n_) *> UIO.succeed(rows_.toArray.apply(i).toArray.apply(j))

  override def toString = rows_.map(_.mkString(" ")).mkString("\n")
}

object Matrix {

  type Row = Matrix
  type Col = Matrix

  private def checkAccessIndex(x: Int, y: Int, rows: Int, cols: Int): IO[IndexExceedMatrixDimension, Unit] =
    if (x >= 0 && x < rows && y >= 0 && y < cols) IO.unit
    else IO.fail(IndexExceedMatrixDimension(x, y, rows, cols))

  def create(m: Int, n: Int, value: Double): UIO[Matrix] = UIO {
    val elems = Chunk.fromArray(Array.fill(m)(Chunk.fromArray(Array.fill(n)(value))))
    new Matrix(m, n, elems)
  }

  def zero(m: Int, n: Int): UIO[Matrix] = create(m, n, 0)
  def ones(m: Int, n: Int): UIO[Matrix] = create(m, n, 1)

  def createRowVector(elems: Chunk[Double]): UIO[Row] = UIO.succeed(new Matrix(1, elems.length, Chunk(elems)))
  def createColVector(elems: Chunk[Double]): UIO[Col] = UIO.succeed(new Matrix(elems.length, 1, elems.map(Chunk(_))))

  def fromRows(m: Int, n: Int, rows: Chunk[Chunk[Double]]): ZIO[Any, MatrixConstructionError, Matrix] =
    for {
      _ <- if (rows.length == m && rows.foldLeft(true)((s, r) => s && r.length == n)) IO.unit
      else IO.fail(MatrixConstructionError(s"can't build a matrix out of these rows as dimensions are not correct: m = $m, n = $n, rows.length = ${rows.length}"))
    } yield
      new Matrix(m, n, rows) {}

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
    def equal(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Boolean]
    def add(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def mul(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def invert(m: Matrix): ZIO[R, MatrixError, Matrix]
  }

  trait LiveMatrixOps extends MatrixOps {
    override def matrixOps: Service[Any] = new Service[Any] {
      override def add(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Matrix] =
        for {
          m1_m    <- m1.m
          m1_n    <- m1.n
          m2_m    <- m2.m
          m2_n    <- m2.n
          _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't add a matrix $m1_m x $m1_n and  a matrix $m2_m x $m2_n)")) else IO.unit
          rows1   <- m1.rows
          rows2   <- m2.rows
          addRows <- UIO(rows1.zipWith(rows2) { case (row1, row2) => row1.zipWith(row2)(_ + _) })
          res     <- Matrix.fromRows(m1_m, m1_n, addRows)
        } yield res

      override def mul(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Matrix] = ???

      override def invert(m: Matrix): ZIO[Any, MatrixError, Matrix] = ???

      override def equal(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Boolean] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        rows1 <- m1.rows
        rows2 <- m2.rows
      } yield rows1 == rows2
    }
  }

  object LiveMatrixOps extends LiveMatrixOps

}
