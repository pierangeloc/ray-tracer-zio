package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO, UIO}
import zio.stream._
import zio._

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

  "all transformations" should {
    "compose" in {
      import AffineTransformations._
      val center = point(0, 0, 0)
      val rotationAngle = scala.math.Pi / 6
      unsafeRun {
        for {
          rotationMatrix   <- rotateZ(rotationAngle)
          scalingMatrix    <- scaling(640 / 2, 480 / 2, 0)
          translationMtx   <- translation(640 / 2, 480 / 2, 0)
          composed1        <- matrixOps.mul(rotationMatrix, scalingMatrix)
          composed2        <- matrixOps.mul(translationMtx, composed1)
          horizontalRadius <- vector(1, 0, 0)
          positions        <- Stream.unfoldM(horizontalRadius)(v => matrixOps.mul(composed2, v).map(vv => Some((vv, vv)))).take(12).run(Sink.collectAll)
          _                <- ZIO.traverse(positions) { pos =>
            for {
              x <- pos.get(0, 0)
              y <- pos.get(1, 0)
              z <- pos.get(2, 0)
              k <- pos.get(3, 0)
              _ <- IO(println(s"($x, $y, $z, $k)"))
            } yield ()
          }
          _                <- console.putStrLn(positions.mkString("\n"))
        } yield ()

      }

    }

  }
}