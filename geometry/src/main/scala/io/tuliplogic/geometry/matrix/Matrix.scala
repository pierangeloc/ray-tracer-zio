package io.tuliplogic.geometry.matrix

import cats.{FlatMap, Functor}
import cats.implicits._
import io.tuliplogic.raytracer.errors.MatrixError
import io.tuliplogic.raytracer.errors.MatrixError.{IndexExceedMatrixDimension, MatrixConstructionError, MatrixDimError}
import zio.{IO, _}
import mouse.all._

import scala.reflect.ClassTag

//matrix m rows by n cols
class Matrix[L[_]] private (private val m_ : Int, n_ : Int, rows_ : L[L[Double]])(implicit L: Vectorizable[L], ct: ClassTag[L[Double]]) {
  private val cols_ : L[L[Double]] = L.toArray(rows_).map(L.toArray).transpose.map(L.fromArray).toArray |> L.fromArray[L[Double]]
//  private val cols_ : Chunk[Chunk[Double]] = rows_.toArray.map(_.toArray).transpose.map(Chunk.fromArray) |> Chunk.fromArray
  def m: UIO[Int]                     = UIO.succeed(m_)
  def n: UIO[Int]                     = UIO.succeed(n_)
  def rows: UIO[L[L[Double]]] = UIO.succeed(rows_)
  def cols: UIO[L[L[Double]]] = UIO.succeed(cols_)
  def transpose: UIO[Matrix[L]]          = UIO.succeed(new Matrix(n_, m_, cols_))
  def get(i: Int, j: Int): IO[IndexExceedMatrixDimension, Double] =
    Matrix.checkAccessIndex(i, j, m_, n_) *>
//    UIO.effectTotal(5.0)
      UIO.effectTotal {
        println(s"i, j: $i, $j")
        println(rows_)
        val res = L.get(L.get(rows_)(i))(j)
        println("result: "+ res)
        res
      }

  override def toString: String = L.toArray(rows_).map(rs => "| " +  L.toArray(rs).mkString(" ") + " |").mkString("\n")
}

object Matrix {

  private def checkAccessIndex(x: Int, y: Int, rows: Int, cols: Int): IO[IndexExceedMatrixDimension, Unit] =
    if (x >= 0 && x < rows && y >= 0 && y < cols) IO.unit
    else IO.fail(IndexExceedMatrixDimension(x, y, rows, cols))

  class MatrixFactory[L[_]](implicit L: Vectorizable[L], F: Functor[L], CT: ClassTag[L[Double]]) {
    type Row = Matrix[L]
    type Col = Matrix[L]

    def hom(m: Int, n: Int, value: Double): UIO[Matrix[L]] = UIO {
      val elems = L.fromArray(Array.fill(m)(L.fromArray(Array.fill(n)(value))))
      new Matrix[L](m, n, elems)
    }

    def zero(m: Int, n: Int): UIO[Matrix[L]] = hom(m, n, 0)
    def ones(m: Int, n: Int): UIO[Matrix[L]] = hom(m, n, 1)
    def eye(n: Int): UIO[Matrix[L]] = fromRows(n, n,
      L.fromArray(Array.tabulate(n,n)((x,y) => if(x==y) 1d else 0d).map(L.fromArray))
    ).orDie

    def createRowVector(elems: L[Double]): UIO[Row] = UIO.succeed(new Matrix[L](1, L.length(elems), L.fromArray(Array(elems))))
    def createColVector(elems: L[Double]): UIO[Col] = UIO.succeed(new Matrix[L](L.length(elems), 1, elems.map(x => L.fromArray(Array(x)))))

    def fromRows(m: Int, n: Int, rows: L[L[Double]]): ZIO[Any, MatrixConstructionError, Matrix[L]] =
      for {
        _ <- if (L.length(rows) == m && L.toArray(rows).forall(r => L.length(r) == n)) IO.unit
        else IO.fail(MatrixConstructionError(s"can't build a matrix out of these rows as dimensions are not correct: m = $m, n = $n, rows.length = ${L.length(rows)}"))
      } yield
        //trying to fix a boxing issue with Chunk //TODO: find a minimal example that shows this weird chunk behavior
        new Matrix(m, n, rows.map(chunk => L.fromArray(L.toArray(chunk)))) {}
  }

}

trait MatrixOps {
  def matrixOps: MatrixOps.Service[Any]
}

object MatrixOps {

  import Types._

  /*
    - add
    - mul
    - invert
   */
  trait Service[R] {
    def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[R, MatrixError, Boolean]
    def opposite(m: M): ZIO[R, MatrixError, M]
    def equal(m1: M, m2: M): ZIO[R, MatrixError, Boolean]
    def add(m1: M, m2: M): ZIO[R, MatrixError, M]
    def mul(m1: M, m2: M): ZIO[R, MatrixError, M]
    def had(m1: M, m2: M): ZIO[R, MatrixError, M]
    def invert(m: M): ZIO[R, MatrixError, M]
  }

  trait LiveMatrixOps extends MatrixOps {

    override def matrixOps: Service[Any] = new Service[Any] {

      private def elementWiseOp(m1: M, m2: M)(op: (Double, Double) => Double): ZIO[Any, MatrixError, M] =
        for {
          m1_m    <- m1.m
          m1_n    <- m1.n
          m2_m    <- m2.m
          m2_n    <- m2.n
          _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't perform element-wise operation on a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
          rows1   <- m1.rows
          rows2   <- m2.rows
          opRows <- UIO(vectorizable.zip(rows1, rows2) { case (row1, row2) => Vectorizable[L].zip(row1, row2)(op) })
          res     <- factory.fromRows(m1_m, m1_n, opRows)
        } yield res

      override def add(m1: M, m2: M): ZIO[Any, MatrixError, M] =
        elementWiseOp(m1, m2)(_ + _)

      override def mul(m1: M, m2: M): ZIO[Any, MatrixError, M] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_n == m2_m)) IO.fail(MatrixDimError(s"can't multiply a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        m1Rows <- m1.rows
        m2Cols <- m2.cols
        tmp = for {
          m1Row <- m1Rows
          m2Col <- m2Cols
        } yield vectorizable.scalarProduct(m1Row, m2Col)
        resultRows = vectorizable.groupChunk(tmp)(m2_n)
        res    <- factory.fromRows(
          m1_m, m2_n,
          resultRows
        )
      } yield res

      override def invert(m: M): ZIO[Any, MatrixError, M] = {
        //cheating here, I don't want to bother coming up with a correct implementation of this, it might take considerable time
        import breeze.linalg._
        for {
          rows    <- m.rows
          nrRows <- m.m
          nrCols <- m.n
          arrayElems: Array[Double] = rows.flatten.toArray
          bm = DenseMatrix.create(nrRows, nrCols, arrayElems)
          inverse <- ZIO.effect(inv(bm)).mapError(_ => MatrixError.MatrixNotInvertible)
          res <- factory.fromRows(nrRows, nrCols, vectorizable.fromArray(inverse.data.grouped(nrCols).map(vectorizable.fromArray).toArray))
        } yield res
      }

      override def equal(m1: M, m2: M): ZIO[Any, MatrixError, Boolean] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        rows1 <- m1.rows
        rows2 <- m2.rows
      } yield rows1 == rows2

      override def almostEqual(m1: M, m2: M, maxMSEr: Double): ZIO[Any, MatrixError, Boolean] = for {
        m1_m    <- m1.m
        m1_n    <- m1.n
        m2_m    <- m2.m
        m2_n    <- m2.n
        _       <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check almost equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
        rows1   <- m1.rows
        rows2   <- m2.rows
        diff    <- UIO.succeed(vectorizable.zip(fm.flatten(rows1), fm.flatten(rows2))(_ - _))
        squaredError <- IO.effectTotal(vectorizable.l2(diff))
      } yield squaredError / (m1_m * m2_n) < maxMSEr

      override def opposite(m: M): ZIO[Any, MatrixError, M] = for {
        m_m  <- m.m
        m_n  <- m.n
        rows <- m.rows
        res  <- factory.fromRows(m_m, m_n, rows.map(_.map(x => -x)))
      } yield res

      override def had(m1: M, m2: M): ZIO[Any, MatrixError, M] =
        elementWiseOp(m1, m2)(_ * _)
    }
  }

  object LiveMatrixOps extends LiveMatrixOps

}

