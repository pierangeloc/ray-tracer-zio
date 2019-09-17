package io.tuliplogic.geometry.matrix

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

  override def toString: String = rows_.map(rs => "| " +  rs.mkString(" ") + " |").mkString("\n")
}

object Matrix {

  type Row = Matrix
  type Col = Matrix

  private def checkAccessIndex(x: Int, y: Int, rows: Int, cols: Int): IO[IndexExceedMatrixDimension, Unit] =
    if (x >= 0 && x < rows && y >= 0 && y < cols) IO.unit
    else IO.fail(IndexExceedMatrixDimension(x, y, rows, cols))

  def hom(m: Int, n: Int, value: Double): UIO[Matrix] = UIO {
    val elems = Chunk.fromArray(Array.fill(m)(Chunk.fromArray(Array.fill(n)(value))))
    new Matrix(m, n, elems)
  }

  def zero(m: Int, n: Int): UIO[Matrix] = hom(m, n, 0)
  def ones(m: Int, n: Int): UIO[Matrix] = hom(m, n, 1)
  def eye(n: Int): UIO[Matrix] = fromRows(n, n,
    Chunk.fromArray(Array.tabulate(n,n)((x,y) => if(x==y) 1d else 0d).map(Chunk.fromArray))
  ).orDie

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

object ChunkUtils {
  def scalarProduct(row: Chunk[Double], col: Chunk[Double]): Double = row.zipWith(col)(_ * _).foldLeft(0d)(_ + _)
  def l2(row: Chunk[Double]): Double = scalarProduct(row, row)
  def groupChunk[A](chunk: Chunk[A])(groupSize: Int): Chunk[Chunk[A]] =
    Chunk.fromIterable(chunk.toArray.grouped(groupSize).map(Chunk.fromArray).toIterable)
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
    def almostEqual(m1: Matrix, m2: Matrix, maxSquaredError: Double): ZIO[R, MatrixError, Boolean]
    def opposite(m: Matrix): ZIO[R, MatrixError, Matrix]
    def equal(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Boolean]
    def add(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def mul(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def had(m1: Matrix, m2: Matrix): ZIO[R, MatrixError, Matrix]
    def invert(m: Matrix): ZIO[R, MatrixError, Matrix]
  }

  trait LiveMatrixOps extends MatrixOps {

    import ChunkUtils._
    override def matrixOps: Service[Any] = new Service[Any] {

      private def elementWiseOp(m1: Matrix, m2: Matrix)(op: (Double, Double) => Double): ZIO[Any, MatrixError, Matrix] =
        for {
          m1_m    <- m1.m
          m1_n    <- m1.n
          m2_m    <- m2.m
          m2_n    <- m2.n
          _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't perform element-wise operation on a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
          rows1   <- m1.rows
          rows2   <- m2.rows
          opRows <- UIO(rows1.zipWith(rows2) { case (row1, row2) => row1.zipWith(row2)(op) })
          res     <- Matrix.fromRows(m1_m, m1_n, opRows)
        } yield res

      override def add(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Matrix] =
        elementWiseOp(m1, m2)(_ + _)

      override def mul(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Matrix] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_n == m2_m)) IO.fail(MatrixDimError(s"can't multuply a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        m1Rows <- m1.rows
        m2Cols <- m2.cols
        tmp = for {
          m1Row <- m1Rows
            m2Col <- m2Cols
        } yield scalarProduct(m1Row, m2Col)
        resultRows = groupChunk(tmp)(m2_n)
        res    <- Matrix.fromRows(
          m1_m, m2_n,
          resultRows
        )
      } yield res

      override def invert(m: Matrix): ZIO[Any, MatrixError, Matrix] = {
        //cheating here, I don't want to bother coming up with a correct implementation of this, it might take considerable time
        import breeze.linalg._
        for {
          rows    <- m.rows
          nrRows <- m.m
          nrCols <- m.n
          arrayElems: Array[Double] = rows.flatten.toArray
          bm = DenseMatrix.create(nrRows, nrCols, arrayElems)
          inverse <- ZIO.effect(inv(bm)).mapError(_ => MatrixError.MatrixNotInvertible)
          res <- io.tuliplogic.geometry.matrix.Matrix.fromRows(nrRows, nrCols, Chunk.fromArray(inverse.data.grouped(nrCols).map(Chunk.fromArray).toArray))
        } yield res
      }

      override def equal(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Boolean] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        rows1 <- m1.rows
        rows2 <- m2.rows
      } yield rows1 == rows2

      override def almostEqual(m1: Matrix, m2: Matrix, maxMSEr: Double): ZIO[Any, MatrixError, Boolean] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check almost equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        rows1   <- m1.rows
        rows2   <- m2.rows
        diff    <- UIO.succeed(rows1.flatten.zipWith(rows2.flatten)(_ - _))
        squaredError <- IO.effectTotal(ChunkUtils.l2(diff))
      } yield squaredError / (m1_m * m2_n) < maxMSEr

      override def opposite(m: Matrix): ZIO[Any, MatrixError, Matrix] = for {
        m_m  <- m.m
        m_n  <- m.n
        rows <- m.rows
        res  <- Matrix.fromRows(m_m, m_n, rows.map(_.map(x => -x)))
      } yield res

      override def had(m1: Matrix, m2: Matrix): ZIO[Any, MatrixError, Matrix] =
        elementWiseOp(m1, m2)(_ * _)
    }
  }

  object LiveMatrixOps extends LiveMatrixOps

}
