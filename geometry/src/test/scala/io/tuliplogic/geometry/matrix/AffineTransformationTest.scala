package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.AffineTransformation._
import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO, UIO}
import zio.stream._
import zio._
import zio.console.Console

class AffineTransformationTest extends WordSpec with GeneratorDrivenPropertyChecks with Generators with DefaultRuntime {
  import Types._

  import LiveMatrixOps.matrixOps

  "translation transformation " should {
    "translate(x, y, z).apply(point) === point + vector(x, y, z)" in {
      forAll {
        for {
          x     <- reasonableDouble
          y     <- reasonableDouble
          z     <- reasonableDouble
          point <- pointGen
        } yield (x, y, z, AffineTransformation.translate(x, y, z), point)
      } {
        case (x, y, z, translation, point) =>
          unsafeRun(
            for {
              transl          <- translation
              result          <- transl.on(point).provide(LiveMatrixOps)
              vectorToBeAdded <- AffineTransformation.vector(x, y, z)
              expected        <- matrixOps.add(vectorToBeAdded.col, point.col)
              eq              <- matrixOps.equal(expected, result.col)
              _               <- IO.effect(eq shouldEqual true)
            } yield ()
          )
      }
    }

    "translate(x, y, z).apply(vector) === vector" in {
      forAll {
        for {
          x      <- reasonableDouble
          y      <- reasonableDouble
          z      <- reasonableDouble
          vector <- vectorGen
        } yield (AffineTransformation.translate(x, y, z), vector)
      } {
        case (translation, vector) =>
          unsafeRun(
            for {
              transl <- translation
              result <- transl.on(vector).provide(LiveMatrixOps)
              eq     <- matrixOps.equal(vector.col, result.col)
              _      <- IO.effect(eq shouldEqual true)
            } yield ()
          )
      }
    }
  }

  "scaling transformation " should {
    "scale(a, b, c).apply(point) === [point.x * a, point.y * b, point.z * c]" in {
      forAll {
        for {
          x     <- reasonableDouble
          y     <- reasonableDouble
          z     <- reasonableDouble
          point <- pointGen
        } yield (x, y, z, AffineTransformation.scale(x, y, z), point)
      } {
        case (x, y, z, scaling, point) =>
          unsafeRun(
            for {
              scal            <- scaling
              result          <- scal.on(point).provide(LiveMatrixOps)
              vectorToBeAdded <- AffineTransformation.point(x, y, z)
              expected        <- matrixOps.had(vectorToBeAdded.col, point.col)
              eq              <- matrixOps.equal(expected, result.col)
              _               <- IO.effect(eq shouldEqual true)
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

  "all transformations" should {
    "compose" in {
      val rotationAngle = scala.math.Pi / 2

      unsafeRun {
        (for {
          horizontalRadius <- point(1, 0, 0)
          rotateTf         <- rotateZ(rotationAngle)
          scaleTf          <- scale(20, 20, 20)
          translateTf      <- translate(10, 30, 0)
          comp             <- composeLeft(rotateTf, scaleTf, translateTf)
          res              <- comp on horizontalRadius
          expected         <- AffineTransformation.point(10, 50, 0)
          _                <- matrixOperations.almostEqual(res.col, expected.col, 10e-10).flatMap(res => IO.effect(res shouldEqual true))
        } yield ()).provide(new LiveMatrixOps with Console.Live {})
      }
    }

  }
}
