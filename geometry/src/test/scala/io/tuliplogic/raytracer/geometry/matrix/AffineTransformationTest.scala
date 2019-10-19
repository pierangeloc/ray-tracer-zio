package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.affine.{affineTfOps, AffineTransformation}
import io.tuliplogic.raytracer.geometry.affine.AffineTransformation._
import io.tuliplogic.raytracer.geometry.affine.AffineTransformationOps.BreezeMatrixOps$
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import mouse.all._
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO, UIO}
import zio.stream._
import zio._
import zio.console.Console

class AffineTransformationTest extends WordSpec with GeneratorDrivenPropertyChecks with Generators with DefaultRuntime {
  import Types._
  val env = new BreezeMatrixOps$ with MatrixModule.BreezeMatrixModule

  "identity transformation " should {
    "identity.apply(point) === point" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf     <- AffineTransformation.id
            result <- affineTfOps.transform(tf, point)
            _      <- IO.effect(result === point)
          } yield ()).provide(env)
        )
      }
    }
  }

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
            (for {
              transl          <- translation
              result          <- affineTfOps.transform(transl, point)
              resultCol       <- result |> toCol
              vectorToBeAdded <- Vec(x, y, z) |> toCol
              pointCol        <- point |> toCol
              expected        <- >.add(vectorToBeAdded, pointCol)
              eq              <- >.equal(expected, resultCol)
              _               <- IO.effect(eq shouldEqual true)
            } yield ()).provide(env)
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
            (for {
              transl    <- translation
              result    <- affineTfOps.transform(transl, vector)
              resultCol <- result |> toCol
              vectorCol <- vector |> toCol
              eq        <- >.equal(vectorCol, resultCol)
              _         <- IO.effect(eq shouldEqual true)
            } yield ()).provide(env)
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
            (for {
              scal            <- scaling
              result          <- affineTfOps.transform(scal, point)
              resultCol       <- result |> toCol
              pointCol        <- point |> toCol
              vectorToBeAdded <- Pt(x, y, z) |> toCol
              expected        <- >.had(vectorToBeAdded, pointCol)
              eq              <- >.equal(expected, resultCol)
              _               <- IO.effect(eq shouldEqual true)
            } yield ()).provide(env)
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
          horizontalRadius <- UIO.succeed(Pt(1, 0, 0))
          rotateTf         <- rotateZ(rotationAngle)
          scaleTf          <- scale(20, 20, 20)
          translateTf      <- translate(10, 30, 0)
          comp             <- composeLeft(rotateTf, scaleTf, translateTf)
          res              <- affineTfOps.transform(comp, horizontalRadius)
          resCol           <- res |> toCol
          expectedCol      <- Pt(10, 50, 0) |> toCol
          _                <- >.almostEqual(resCol, expectedCol, 10e-10).flatMap(res => IO.effect(res shouldEqual true))
        } yield ()).provide(env)
      }
    }

  }
}
