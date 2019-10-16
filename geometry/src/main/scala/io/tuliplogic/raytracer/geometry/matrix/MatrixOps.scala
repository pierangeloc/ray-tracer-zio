package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.matrix.Types.M
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.commons.errors.AlgebraicError.MatrixDimError
import zio.{IO, UIO, ZIO}

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
    def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[R, AlgebraicError, Boolean]
    def opposite(m: M): ZIO[R, AlgebraicError, M]
    def equal(m1: M, m2: M): ZIO[R, AlgebraicError, Boolean]
    def add(m1: M, m2: M): ZIO[R, AlgebraicError, M]
    def mul(m1: M, m2: M): ZIO[R, AlgebraicError, M]
    def had(m1: M, m2: M): ZIO[R, AlgebraicError, M]
    def invert(m: M): ZIO[R, AlgebraicError, M]

    def times(α: Double, m: M): ZIO[R, Nothing, M] =
      for {
        m_m   <- m.m
        m_n   <- m.n
        other <- factory.hom(m_m, m_n, α)
        res   <- had(m, other).orDie
      } yield res
  }

  trait Live extends MatrixOps {

    override def matrixOps: Service[Any] = new Service[Any] {

      private def elementWiseOp(m1: M, m2: M)(op: (Double, Double) => Double): ZIO[Any, AlgebraicError, M] =
        for {
          m1_m <- m1.m
          m1_n <- m1.n
          m2_m <- m2.m
          m2_n <- m2.n
          _ <- if (!(m1_m == m2_m && m1_n == m2_n))
            IO.fail(MatrixDimError(s"can't perform element-wise operation on a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)"))
          else IO.unit
          rows1  <- m1.rows
          rows2  <- m2.rows
          opRows <- UIO(vectorizable.zip(rows1, rows2) { case (row1, row2) => Vectorizable[L].zip(row1, row2)(op) })
          res    <- factory.fromRows(m1_m, m1_n, opRows)
        } yield res

      override def add(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
        elementWiseOp(m1, m2)(_ + _)

      override def mul(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
        for {
          m1_m   <- m1.m
          m1_n   <- m1.n
          m2_m   <- m2.m
          m2_n   <- m2.n
          _      <- if (!(m1_n == m2_m)) IO.fail(MatrixDimError(s"can't multiply a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
          m1Rows <- m1.rows
          m2Cols <- m2.cols
          tmp = for {
            m1Row <- m1Rows
            m2Col <- m2Cols
          } yield vectorizable.scalarProduct(m1Row, m2Col)
          resultRows = vectorizable.groupChunk(tmp)(m2_n)
          res <- factory.fromRows(
            m1_m,
            m2_n,
            resultRows
          )
        } yield res

      override def invert(m: M): ZIO[Any, AlgebraicError, M] = {
        //cheating here, I don't want to bother coming up with a correct implementation of this, it might take considerable time
        import breeze.linalg._
        for {
          rows   <- m.rows
          nrRows <- m.m
          nrCols <- m.n
          arrayElems: Array[Double] = rows.flatten.toArray
          bm                        = DenseMatrix.create(nrRows, nrCols, arrayElems)
          inverse <- ZIO.effect(inv(bm)).mapError(_ => AlgebraicError.MatrixNotInvertible)
          res     <- factory.fromRows(nrRows, nrCols, vectorizable.fromArray(inverse.data.grouped(nrCols).map(vectorizable.fromArray).toArray))
        } yield res
      }

      override def equal(m1: M, m2: M): ZIO[Any, AlgebraicError, Boolean] =
        for {
          m1_m <- m1.m
          m1_n <- m1.n
          m2_m <- m2.m
          m2_n <- m2.n
          _ <- if (!(m1_m == m2_m && m1_n == m2_n)) IO.fail(MatrixDimError(s"can't check equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)"))
          else IO.unit
          rows1 <- m1.rows
          rows2 <- m2.rows
        } yield rows1 == rows2

      override def almostEqual(m1: M, m2: M, maxMSEr: Double): ZIO[Any, AlgebraicError, Boolean] =
        for {
          m1_m <- m1.m
          m1_n <- m1.n
          m2_m <- m2.m
          m2_n <- m2.n
          _ <- if (!(m1_m == m2_m && m1_n == m2_n))
            IO.fail(MatrixDimError(s"can't check almost equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)"))
          else IO.unit
          rows1        <- m1.rows
          rows2        <- m2.rows
          diff         <- UIO.succeed(vectorizable.zip(fm.flatten(rows1), fm.flatten(rows2))(_ - _))
          squaredError <- IO.effectTotal(vectorizable.l2(diff))
        } yield squaredError / (m1_m * m2_n) < maxMSEr

      override def opposite(m: M): ZIO[Any, AlgebraicError, M] =
        for {
          m_m  <- m.m
          m_n  <- m.n
          rows <- m.rows
          res  <- factory.fromRows(m_m, m_n, rows.map(_.map(x => -x)))
        } yield res

      override def had(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
        elementWiseOp(m1, m2)(_ * _)
    }
  }

  object Live extends Live

}

object matrixOperations extends MatrixOps.Service[MatrixOps] {
  override def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[MatrixOps, AlgebraicError, Boolean] =
    ZIO.accessM(_.matrixOps.almostEqual(m1, m2, maxSquaredError))

  override def opposite(m: M): ZIO[MatrixOps, AlgebraicError, M] =
    ZIO.accessM(_.matrixOps.opposite(m))

  override def equal(m1: M, m2: M): ZIO[MatrixOps, AlgebraicError, Boolean] =
    ZIO.accessM(_.matrixOps.equal(m1, m2))

  override def add(m1: M, m2: M): ZIO[MatrixOps, AlgebraicError, M] =
    ZIO.accessM(_.matrixOps.add(m1, m2))

  override def mul(m1: M, m2: M): ZIO[MatrixOps, AlgebraicError, M] =
    ZIO.accessM(_.matrixOps.mul(m1, m2))

  override def had(m1: M, m2: M): ZIO[MatrixOps, AlgebraicError, M] =
    ZIO.accessM(_.matrixOps.had(m1, m2))

  override def invert(m: M): ZIO[MatrixOps, AlgebraicError, M] =
    ZIO.accessM(_.matrixOps.invert(m))
}
