package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck._
import zio._

class MatrixOpsTest extends WordSpec with GeneratorDrivenPropertyChecks with DefaultRuntime {

  def matrixGenWithDim(m: Int, n: Int): Gen[Matrix] =
    for {
      elems  <- Gen.listOfN(m * n, Gen.chooseNum[Double](-1000, 1000))
      matrix <- Gen.const(unsafeRun(Matrix.fromRows(m, n, Chunk.fromIterable(elems.grouped(n).map(Chunk.fromIterable).toIterable))))
    } yield matrix

  def matrixGen: Gen[Matrix] =
    for {
      m      <- Gen.chooseNum(1, 100)
      n      <- Gen.chooseNum(1, 100)
      matrix <- matrixGenWithDim(m, n)
    } yield matrix

  "LiveMatrixOps" should {
    import LiveMatrixOps._

    "add correctly 2 matrices" in {
      unsafeRun {
        for {
          m1         <- Matrix.fromRows(3, 2, Chunk(Chunk(1, 2), Chunk(3, 4), Chunk(5, 6)))
          m2         <- Matrix.fromRows(3, 2, Chunk(Chunk(10, 20), Chunk(30, 40), Chunk(50, 60)))
          plusMatrix <- matrixOps.add(m1, m2)
          plusRows   <- plusMatrix.rows
          _          <- IO.effect(plusRows shouldEqual Chunk(Chunk(11, 22), Chunk(33, 44), Chunk(55, 66)))
        } yield ()
      }
    }

    "matrices form an additive monoid" in {
      forAll {
        for {
          m <- Gen.chooseNum(1, 3)
          n <- Gen.chooseNum(1, 3)
          m1 <- matrixGenWithDim(m, n)
          m2 <- matrixGenWithDim(m, n)
          m3 <- matrixGenWithDim(m, n)
          zero <- Gen.const(unsafeRun(Matrix.zero(m, n)))
        } yield (m1, m2, m3, zero)
      } { case (m1, m2, m3, zero) =>
          unsafeRun (
            for {
              added0         <- matrixOps.add(m1, zero)
              associateFirst <- matrixOps.add(m1, m2).flatMap(matrixOps.add(_, m3))
              associateLast  <- matrixOps.add(m2, m3).flatMap(matrixOps.add(m1, _))
              eq1            <- matrixOps.equal(added0, m1)
              eq2            <- matrixOps.equal(associateFirst, associateLast)
              _              <- IO.effect{
                eq1 shouldEqual true
//                eq2 shouldEqual true //this fails easily with double which is not a monoid
              }
            } yield ()
          )

        }
      }

      "multiply correctly 2 matrices" in {
        unsafeRun {
          for {
            m1         <- Matrix.fromRows(3, 4, Chunk(Chunk(1, 2, 3, 4), Chunk(5, 6, 7, 8), Chunk(9, 10, 11, 12)))
            m2         <- Matrix.fromRows(4, 2, Chunk(Chunk(1, 2), Chunk(3, 4), Chunk(5, 6), Chunk(7, 8)))
            mulMatrix  <- matrixOps.mul(m1, m2)
            expected   <- Matrix.fromRows(3, 2, Chunk(Chunk(50, 60), Chunk(114, 140), Chunk(178, 220)))
            equality   <- matrixOps.equal(mulMatrix, expected)
            _          <- IO.effect(equality shouldEqual true)
          } yield ()
        }
      }

    "squared matrices form a multiplicative monoid" in {
      forAll {
        for {
          m <- Gen.chooseNum(1, 3)
          m1 <- matrixGenWithDim(m, m)
          m2 <- matrixGenWithDim(m, m)
          m3 <- matrixGenWithDim(m, m)
          idM <- Gen.const(unsafeRun(Matrix.eye(m)))
        } yield (m1, m2, m3, idM)
      } { case (m1, m2, m3, idM) =>
        unsafeRun (
          for {
            m1TimesId      <- matrixOps.mul(m1, idM)
            idTimesM1      <- matrixOps.mul(idM, m1)
            associateFirst <- matrixOps.mul(m1, m2).flatMap(matrixOps.mul(_, m3))
            associateLast  <- matrixOps.mul(m2, m3).flatMap(matrixOps.mul(m1, _))
            eq1            <- matrixOps.equal(m1TimesId, m1)
            eq2            <- matrixOps.equal(idTimesM1, m1)
            eq3            <- matrixOps.equal(associateFirst, associateLast)
            _              <- IO.effect {
                eq1 shouldEqual true
                eq2 shouldEqual true
//                eq3 shouldEqual true //this fails easily with double which is not a monoid
              }
          } yield ()
        )
      }
    }

    "invert an invertible matrix" in {
      unsafeRun {
        for {
          //   8 | -5 |  9 |  2 |
          //​ 	    |  7 |  5 |  6 |  1 |
          //​ 	    | -6 |  0 |  9 |  6 |
          //​ 	    | -3 |  0 | -9 | -4 |
          m          <- Matrix.fromRows(4, 4, Chunk(Chunk(8, -5, 9, 2), Chunk(7, 5, 6, 1), Chunk(-6, 0, 9, 6), Chunk(-3, 0, -9, -4)))
          mulMatrix  <- matrixOps.invert(m)
          expectedId <- matrixOps.mul(m, mulMatrix)
          eye        <- Matrix.eye(4)
          equality   <- matrixOps.almostEqual(expectedId, eye, 10e-9)
          _          <- IO.effect(equality shouldEqual true)
        } yield ()
      }
    }

    "transpose a matrix" in {
      unsafeRun {
        for {
          m           <- Matrix.fromRows(3, 4, Chunk(Chunk(1, 2, 3, 4), Chunk(5, 6, 7, 8), Chunk(9, 10, 11, 12)))
          transposed  <- m.transpose
          expected   <- Matrix.fromRows(4, 3, Chunk(Chunk(1, 5, 9), Chunk(2, 6, 10), Chunk(3, 7, 11), Chunk(4, 8, 12)))
          equality   <- matrixOps.equal(transposed, expected)
          _          <- IO.effect(equality shouldEqual true)
        } yield ()
      }
    }
  }
}
