package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.matrix.Types.{M, factory}
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.commons.errors.AlgebraicError.MatrixDimError
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule.MatrixTestService.Op
//import zio.macros.mock.mockable
import zio.{IO, Ref, UIO, ZIO}

//@mockable
//we don't make this mockable just to show the testing pattern
trait MatrixModule {
  val matrixModule: MatrixModule.Service[Any]
}

object MatrixModule {

  object syntax {
    implicit class RichService[R](s: Service[R]) {
      def times(α: Double, m: M): ZIO[R, Nothing, M] =
        for {
          m_m   <- m.m
            m_n   <- m.n
            other <- factory.hom(m_m, m_n, α)
            res   <- s.had(m, other).orDie
        } yield res
    }
  }

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
  }

  trait BreezeLive extends MatrixModule {

    override val matrixModule: Service[Any] = new Service[Any] {

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

      def add(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
        elementWiseOp(m1, m2)(_ + _)

      def mul(m1: M, m2: M): ZIO[Any, AlgebraicError, M] = {
        import breeze.linalg._
        for {
          m1_m   <- m1.m
          m1_n   <- m1.n
          m2_m   <- m2.m
          m2_n   <- m2.n
          _      <- if (!(m1_n == m2_m)) IO.fail(MatrixDimError(s"can't multiply a matrix $m1_m x $m1_n and a matrix $m2_m x $m2_n)")) else IO.unit
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

      def invert(m: M): ZIO[Any, AlgebraicError, M] = {
        //cheating here, I don't want to bother coming up with a correct implementation of this, it might take considerable time
        import breeze.linalg._
        for {
          rows   <- m.rows
          nrRows <- m.m
          nrCols <- m.n
          arrayElems: Array[Double] = rows.flatten.toArray
          bm                        = DenseMatrix.create(nrRows, nrCols, arrayElems)
          inverse <- ZIO.effect(inv(bm)).mapError(_ => AlgebraicError.MatrixNotInvertible(m.toString))
          res     <- factory.fromRows(nrRows, nrCols, vectorizable.fromArray(inverse.data.grouped(nrCols).map(vectorizable.fromArray).toArray))
        } yield res
      }

      def equal(m1: M, m2: M): ZIO[Any, AlgebraicError, Boolean] =
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

      def almostEqual(m1: M, m2: M, maxMSEr: Double): ZIO[Any, AlgebraicError, Boolean] =
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

      def opposite(m: M): ZIO[Any, AlgebraicError, M] =
        for {
          m_m  <- m.m
          m_n  <- m.n
          rows <- m.rows
          res  <- factory.fromRows(m_m, m_n, rows.map(_.map(x => -x)))
        } yield res

      def had(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
        elementWiseOp(m1, m2)(_ * _)
    }
  }

  object BreezeLive extends BreezeLive


  case class MatrixTestService(ref: Ref[MatrixTestService.State]) extends MatrixModule.Service[Any] {
    override def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[Any, AlgebraicError, Boolean] = ZIO.die(new IllegalArgumentException("not implemented"))
    override def opposite(m: M): ZIO[Any, AlgebraicError, M] = ZIO.die(new IllegalArgumentException("not implemented"))
    override def equal(m1: M, m2: M): ZIO[Any, AlgebraicError, Boolean] = ZIO.die(new IllegalArgumentException("not implemented"))
    override def add(m1: M, m2: M): ZIO[Any, AlgebraicError, M] = ref.modify(_.findOp(Op.Add(m1, m2))).flatMap(_.get)
    override def mul(m1: M, m2: M): ZIO[Any, AlgebraicError, M] = ref.modify(_.findOp(Op.Mul(m1, m2))).flatMap(_.get)
    override def had(m1: M, m2: M): ZIO[Any, AlgebraicError, M] = ref.modify(_.findOp(Op.Had(m1, m2))).flatMap(_.get)
    override def invert(m: M): ZIO[Any, AlgebraicError, M] = ref.modify(_.findOp(Op.Inv(m))).flatMap(_.get)
  }

  object MatrixTestService {
    sealed trait Op extends Product with Serializable
    object Op {
      case class Add(m1: M, m2: M) extends Op
      case class Mul(m1: M, m2: M) extends Op
      case class Had(m1: M, m2: M) extends Op
      case class Inv(m: M) extends Op
    }

    final case class State(preLoaded: Map[Op, IO[AlgebraicError, M]], calls: List[String]) {
      def log(op: String): State = copy(calls = op :: calls)
      def findOp(op: Op): (Option[IO[AlgebraicError, M]], State) = {
        println(s"finding op $op among preloaded $preLoaded")
        (preLoaded.get(op), log(op.toString))
      }
    }
  }

  // access helper
  object > extends MatrixModule.Service[MatrixModule] {
    override def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[MatrixModule, AlgebraicError, Boolean] =
      ZIO.accessM(_.matrixModule.almostEqual(m1, m2, maxSquaredError))

    override def opposite(m: M): ZIO[MatrixModule, AlgebraicError, M] =
      ZIO.accessM(_.matrixModule.opposite(m))

    override def equal(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, Boolean] =
      ZIO.accessM(_.matrixModule.equal(m1, m2))

    override def add(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
      ZIO.accessM(_.matrixModule.add(m1, m2))

    override def mul(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
      ZIO.accessM(_.matrixModule.mul(m1, m2))

    override def had(m1: M, m2: M): ZIO[MatrixModule, AlgebraicError, M] =
      ZIO.accessM(_.matrixModule.had(m1, m2))

    override def invert(m: M): ZIO[MatrixModule, AlgebraicError, M] =
      ZIO.accessM(_.matrixModule.invert(m))
  }
}

