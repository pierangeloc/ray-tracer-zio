package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.matrix.Types.{M, factory}
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.commons.errors.AlgebraicError.MatrixDimError
import zio.{Has, IO, Layer, UIO, ZIO, ZLayer}

object matrixModule {

  trait Service {
    def almostEqual(m1: M, m2: M, maxSquaredError: Double): IO[AlgebraicError, Boolean]

    def opposite(m: M): IO[AlgebraicError, M]

    def equal(m1: M, m2: M): IO[AlgebraicError, Boolean]

    def add(m1: M, m2: M): IO[AlgebraicError, M]

    def mul(m1: M, m2: M): IO[AlgebraicError, M]

    def had(m1: M, m2: M): IO[AlgebraicError, M]

    def invert(m: M): IO[AlgebraicError, M]
  }

  type MatrixModule = Has[Service]

  object syntax {

    implicit class RichService(s: Service) {
      def times(α: Double, m: M): UIO[M] =
      for {
              m_m <- m.m
                m_n <- m.n
                other <- factory.hom(m_m, m_n, α)
                res <- s.had(m, other).orDie
        } yield res
    }

  }

  import Types._

  val breezeLive: Layer[Nothing, MatrixModule] = ZLayer.succeed {
      new Service {
        private def elementWiseOp(m1: M, m2: M)(op: (Double, Double) => Double): IO[AlgebraicError, M] =
          for {
            m1_m <- m1.m
              m1_n <- m1.n
              m2_m <- m2.m
              m2_n <- m2.n
              _ <- if (!(m1_m == m2_m && m1_n == m2_n))
                IO.fail(MatrixDimError(s"can't perform element-wise operation on a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)"))
              else IO.unit
              rows1 <- m1.rows
              rows2 <- m2.rows
              opRows <- UIO(vectorizable.zip(rows1, rows2) { case (row1, row2) => Vectorizable[L].zip(row1, row2)(op) })
              res <- factory.fromRows(m1_m, m1_n, opRows)
          } yield res

        def add(m1: M, m2: M): IO[AlgebraicError, M] =
          elementWiseOp(m1, m2)(_ + _)

        def mul(m1: M, m2: M): IO[AlgebraicError, M] = {
          import breeze.linalg._
          for {
            m1_m <- m1.m
              m1_n <- m1.n
              m2_m <- m2.m
              m2_n <- m2.n
              _ <- if (!(m1_n == m2_m)) IO.fail(MatrixDimError(s"can't multiply a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
              m1Cols <- m1.cols
              m2Cols <- m2.cols
              m1Elems: Array[Double] = m1Cols.flatten.toArray
              m2Elems: Array[Double] = m2Cols.flatten.toArray
              m1B = DenseMatrix.create(m1_m, m1_n, m1Elems)
              m2B = DenseMatrix.create(m2_m, m2_n, m2Elems)
              mul <- ZIO.effectTotal(m1B * m2B)

              res <- factory.fromRows(
                m1_m,
                m2_n,
                vectorizable.fromArray(mul.data.grouped(m1_m).toArray.transpose.map(vectorizable.fromArray))
              )
          } yield res
        }

        def invert(m: M): IO[AlgebraicError, M] = {
          //cheating here, I don't want to bother coming up with a correct implementation of this, it might take considerable time
          import breeze.linalg._
          for {
            rows <- m.rows
              nrRows <- m.m
              nrCols <- m.n
              arrayElems: Array[Double] = rows.flatten.toArray
              bm = DenseMatrix.create(nrRows, nrCols, arrayElems)
              inverse <- ZIO.effect(inv(bm)).mapError(_ => AlgebraicError.MatrixNotInvertible(m.toString))
              res <- factory.fromRows(nrRows, nrCols, vectorizable.fromArray(inverse.data.grouped(nrCols).map(vectorizable.fromArray).toArray))
          } yield res
        }

        def equal(m1: M, m2: M): IO[AlgebraicError, Boolean] =
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

        def almostEqual(m1: M, m2: M, maxMSEr: Double): IO[AlgebraicError, Boolean] =
          for {
            m1_m <- m1.m
              m1_n <- m1.n
              m2_m <- m2.m
              m2_n <- m2.n
              _ <- if (!(m1_m == m2_m && m1_n == m2_n))
                IO.fail(MatrixDimError(s"can't check almost equality of a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)"))
              else IO.unit
              rows1 <- m1.rows
              rows2 <- m2.rows
              diff <- UIO.succeed(vectorizable.zip(fm.flatten(rows1), fm.flatten(rows2))(_ - _))
              squaredError <- IO.effectTotal(vectorizable.l2(diff))
          } yield squaredError / (m1_m * m2_n) < maxMSEr

        def opposite(m: M): IO[AlgebraicError, M] =
          for {
            m_m <- m.m
              m_n <- m.n
              rows <- m.rows
              res <- factory.fromRows(m_m, m_n, rows.map(_.map(x => -x)))
          } yield res

        def had(m1: M, m2: M): IO[AlgebraicError, M] =
          elementWiseOp(m1, m2)(_ * _)
      }
    }

  def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[MatrixModule, AlgebraicError, Boolean] =
    ZIO.accessM(_.get.almostEqual(m1, m2, maxSquaredError))

  def opposite(m: M): ZIO[MatrixModule, AlgebraicError, M] =
    ZIO.accessM(_.get.opposite(m))

  def equal(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, Boolean] =
    ZIO.accessM(_.get.equal(m1, m2))

  def add(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
    ZIO.accessM(_.get.add(m1, m2))

  def mul(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
    ZIO.accessM(_.get.mul(m1, m2))

  def had(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
    ZIO.accessM(_.get.had(m1, m2))

  def invert(m: M): ZIO[MatrixModule, AlgebraicError, M] =
    ZIO.accessM(_.get.invert(m))
}

