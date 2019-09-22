package io.tuliplogic.geometry.matrix

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
          x <- reasonableDouble
          y <- reasonableDouble
          z <- reasonableDouble
          point <- pointGen
        } yield (x, y, z, AffineTransformation.translate(x, y, z), point)
      } { case (x, y, z, translation, point) =>
        unsafeRun(
          for {
            transl <- translation
            result <- transl.on(point).provide(LiveMatrixOps)
            vectorToBeAdded <- AffineTransformation.vector(x, y, z)
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
        } yield (AffineTransformation.translate(x, y, z), vector)
      } { case (translation, vector) =>
        unsafeRun(
          for {
            transl <- translation
              result <- transl.on(vector).provide(LiveMatrixOps)
              eq <- matrixOps.equal(vector, result)
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
        } yield (x, y, z, AffineTransformation.scale(x, y, z), point)
      } { case (x, y, z, scaling, point) =>
        unsafeRun(
          for {
            scal <- scaling
            result <- scal.on(point).provide(LiveMatrixOps)
            vectorToBeAdded <- AffineTransformation.point(x, y, z)
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
      import AffineTransformation._
      val center = point(0, 0, 0)
      val rotationAngle = scala.math.Pi / 6
      unsafeRun {
        (for {
          rotateTf         <- rotateZ(rotationAngle)
          scaleTf          <- scale(640 / 2, 480 / 2, 0)
          translateTf      <- translate(640 / 2, 480 / 2, 0)
          composed         <- AffineTransformation.composeLeft(rotateTf, scaleTf, translateTf)
          horizontalRadius <- vector(1, 0, 0)
          str = ZStream.unfoldM(horizontalRadius)(v => composed.on(v).map(vv => Some((vv, vv)))).take(12)
          positions        <- str.run(Sink.collectAll[M])
          _                <- ZIO.traverse(positions) { pos =>
            for {
              x <- pos.get(0, 0)
              y <- pos.get(1, 0)
              z <- pos.get(2, 0)
              k <- pos.get(3, 0)
              _ <- IO(println(s"($x, $y, $z, $k)"))
            } yield ()
          }
        } yield ()).provide(LiveMatrixOps)

      }

    }

  }
}