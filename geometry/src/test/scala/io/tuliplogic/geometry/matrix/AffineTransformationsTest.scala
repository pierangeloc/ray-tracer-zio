package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO}

class AffineTransformationsTest extends WordSpec with GeneratorDrivenPropertyChecks with Generators with DefaultRuntime {

  import LiveMatrixOps.matrixOps

  "translation transformation " should {
    "translate(x, y, z).apply(point) === point + vector(x, y, z)" in {
      forAll {
        for {
          x <- reasonableDouble
            y <- reasonableDouble
            z <- reasonableDouble
            point <- pointGen
        } yield (x, y, z, AffineTransformations.translation(x, y, z), point)
      } { case (x, y, z, translation, point) =>
        unsafeRun(
          for {
            transl <- translation
              result <- matrixOps.mul(transl, point)
              vectorToBeAdded <- AffineTransformations.vector(x, y, z)
              expected <- matrixOps.add(vectorToBeAdded, point)
              eq <- matrixOps.equal(expected, result)
              _ <- IO.effect(eq shouldEqual true)
          } yield ()
        )
      }
    }

    "translate(x, y, z).apply(vector) === vector" in {
      forAll {
        for {
          x <- reasonableDouble
            y <- reasonableDouble
            z <- reasonableDouble
            vector <- vectorGen
        } yield (AffineTransformations.translation(x, y, z), vector)
      } { case (translation, vector) =>
        unsafeRun(
          for {
            transl <- translation
              result <- matrixOps.mul(transl, vector)
              eq <- matrixOps.equal(vector, result)
              _ <- IO.effect(eq shouldEqual true)
          } yield ()
        )
      }
    }

    "invert(translate(x, y, z)).apply(point) === point + vector(-x, -y, -z)" in {
      forAll {
        for {
          x <- reasonableDouble
            y <- reasonableDouble
            z <- reasonableDouble
            point <- pointGen
        } yield (x, y, z, AffineTransformations.translation(x, y, z).flatMap(matrixOps.invert), point)
      } { case (x, y, z, inverted, point) =>
        unsafeRun(
          for {
            inv <- inverted
              result <- matrixOps.mul(inv, point)
              vectorToBeAdded <- AffineTransformations.vector(-x, -y, -z)
              expected <- matrixOps.add(vectorToBeAdded, point)
              eq <- matrixOps.almostEqual(expected, result, 10e-15)
              _ <- IO.effect(eq shouldEqual true)
          } yield ()
        )
      }
    }
  }

  "scaling transformation " should {
    "scale(a, b, c).apply(point) === [point.x * a, point.y * b, point.z * c]" in {
      forAll {
        for {
          x <- reasonableDouble
          y <- reasonableDouble
          z <- reasonableDouble
          point <- pointGen
        } yield (x, y, z, AffineTransformations.scaling(x, y, z), point)
      } { case (x, y, z, scaling, point) =>
        unsafeRun(
          for {
            scal <- scaling
            result <- matrixOps.mul(scal, point)
            vectorToBeAdded <- AffineTransformations.point(x, y, z)
            expected <- matrixOps.had(vectorToBeAdded, point)
            eq <- matrixOps.equal(expected, result)
            _ <- IO.effect(eq shouldEqual true)
          } yield ()
        )
      }
    }

  }

  "rotate transformation" should {
    "" in {
      pending
    }
  }

  "shearing transformation" should {
    "" in {
      pending
    }
  }
}