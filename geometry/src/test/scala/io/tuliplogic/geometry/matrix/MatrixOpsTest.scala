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

    "add correctly 2 matrices" in {
      unsafeRun {
        for {
          m1         <- Matrix.fromRows(3, 2, Chunk(Chunk(1, 2), Chunk(3, 4), Chunk(5, 6)))
          m2         <- Matrix.fromRows(3, 2, Chunk(Chunk(10, 20), Chunk(30, 40), Chunk(50, 60)))
          plusMatrix <- LiveMatrixOps.matrixOps.add(m1, m2)
          plusRows   <- plusMatrix.rows
          _          <- IO.effect(plusRows shouldEqual Chunk(Chunk(11, 22), Chunk(33, 44), Chunk(55, 66)))
        } yield ()
      }
    }

    import LiveMatrixOps._
    "matrices form an additive monoid" ignore { //this fails easily with double
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
                eq2 shouldEqual true
              }
            } yield ()
          )

        }
      }

    }

  }
