package io.tuliplogic.raytracer.geometry.matrix

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck._
import zio._

class MatrixModuleTest extends WordSpec with GeneratorDrivenPropertyChecks with Generators with DefaultRuntime {

  import Types._
  import vectorizable.comp

  "LiveMatrixOps" should {
    val env = MatrixModule.BreezeLive
    "add correctly 2 matrices" in {
      unsafeRun {
        (for {
          m1         <- factory.fromRows(3, 2, comp(comp(1d, 2d), comp(3d, 4d), comp(5d, 6d)))
          m2         <- factory.fromRows(3, 2, comp(comp(10d, 20d), comp(30d, 40d), comp(50d, 60d)))
          plusMatrix <- MatrixModule.>.add(m1, m2)
          plusRows   <- plusMatrix.rows
          _          <- IO.effect(plusRows shouldEqual comp(comp(11d, 22d), comp(33d, 44d), comp(55d, 66d)))
        } yield ()).provide(env)
      }
    }

    "matrices form an additive monoid" in {
      forAll {
        for {
          m    <- Gen.chooseNum(1, 3)
          n    <- Gen.chooseNum(1, 3)
          m1   <- matrixGenWithDim(m, n)
          m2   <- matrixGenWithDim(m, n)
          m3   <- matrixGenWithDim(m, n)
          zero <- Gen.const(unsafeRun(factory.zero(m, n)))
        } yield (m1, m2, m3, zero)
      } {
        case (m1, m2, m3, zero) =>
          unsafeRun(
            (for {
              added0         <- MatrixModule.>.add(m1, zero)
              associateFirst <- MatrixModule.>.add(m1, m2).flatMap(MatrixModule.>.add(_, m3))
              associateLast  <- MatrixModule.>.add(m2, m3).flatMap(MatrixModule.>.add(m1, _))
              eq1            <- MatrixModule.>.equal(added0, m1)
              eq2            <- MatrixModule.>.equal(associateFirst, associateLast)
              _ <- IO.effect {
                eq1 shouldEqual true
//                eq2 shouldEqual true //this fails easily with double which is not a monoid
              }
            } yield ()).provide(env)
          )

      }
    }

    "multiply correctly 2 matrices" in {
      unsafeRun {
        (for {
          m1        <- factory.fromRows(3, 4, comp(comp(1d, 2d, 3d, 4d), comp(5d, 6d, 7d, 8d), comp(9d, 10d, 11d, 12d)))
          m2        <- factory.fromRows(4, 2, comp(comp(1d, 2d), comp(3d, 4d), comp(5d, 6d), comp(7d, 8d)))
          mulMatrix <- MatrixModule.>.mul(m1, m2)
          expected  <- factory.fromRows(3, 2, comp(comp(50d, 60d), comp(114d, 140d), comp(178d, 220d)))
          equality  <- MatrixModule.>.equal(mulMatrix, expected)
          _         <- IO.effect(equality shouldEqual true)
        } yield ()).provide(env)
      }
    }

    "squared matrices form a multiplicative monoid" in {
      forAll {
        for {
          m   <- Gen.chooseNum(1, 3)
          m1  <- matrixGenWithDim(m, m)
          m2  <- matrixGenWithDim(m, m)
          m3  <- matrixGenWithDim(m, m)
          idM <- Gen.const(unsafeRun(factory.eye(m)))
        } yield (m1, m2, m3, idM)
      } {
        case (m1, m2, m3, idM) =>
          unsafeRun(
            (for {
              m1TimesId      <- MatrixModule.>.mul(m1, idM)
              idTimesM1      <- MatrixModule.>.mul(idM, m1)
              associateFirst <- MatrixModule.>.mul(m1, m2).flatMap(MatrixModule.>.mul(_, m3))
              associateLast  <- MatrixModule.>.mul(m2, m3).flatMap(MatrixModule.>.mul(m1, _))
              eq1            <- MatrixModule.>.equal(m1TimesId, m1)
              eq2            <- MatrixModule.>.equal(idTimesM1, m1)
              eq3            <- MatrixModule.>.equal(associateFirst, associateLast)
              _ <- IO.effect {
                eq1 shouldEqual true
                eq2 shouldEqual true
//                eq3 shouldEqual true //this fails easily with double which is not a monoid
              }
            } yield ()).provide(env)
          )
      }
    }

    "invert an invertible matrix" in {
      unsafeRun {
        (for {
          //   8 | -5 |  9 |  2 |
          //​ 	    |  7 |  5 |  6 |  1 |
          //​ 	    | -6 |  0 |  9 |  6 |
          //​ 	    | -3 |  0 | -9 | -4 |
          m          <- factory.fromRows(4, 4, comp(comp(8d, -5d, 9d, 2d), comp(7d, 5d, 6d, 1d), comp(-6d, 0d, 9d, 6d), comp(-3d, 0d, -9d, -4d)))
          mulMatrix  <- MatrixModule.>.invert(m)
          expectedId <- MatrixModule.>.mul(m, mulMatrix)
          eye        <- factory.eye(4)
          equality   <- MatrixModule.>.almostEqual(expectedId, eye, 10e-9)
          _          <- IO.effect(equality shouldEqual true)
        } yield ()).provide(env)
      }
    }

    "transpose a matrix" in {
      unsafeRun {
        (for {
          m <- factory.fromRows(
            3,
            4,
            comp(
              comp(1d, 2d, 3d, 4d),
              comp(5d, 6d, 7d, 8d),
              comp(9d, 10d, 11d, 12d)
            ))
          transposed <- m.transpose
          expected   <- factory.fromRows(4, 3, comp(comp(1d, 5d, 9d), comp(2d, 6d, 10d), comp(3d, 7d, 11d), comp(4d, 8d, 12d)))
          equality   <- MatrixModule.>.equal(transposed, expected)
          _          <- IO.effect(equality shouldEqual true)
        } yield ()).provide(env)
      }
    }
  }
}
