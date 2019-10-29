package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.affine.{ATModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import mouse.all._
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO, UIO}

class AffineTransformationTest extends WordSpec with GeneratorDrivenPropertyChecks with Generators with DefaultRuntime {
  import Types._
  val env = new ATModule.Live with MatrixModule.BreezeMatrixModule

  "identity transformation " should {
    "identity.apply(point) === point" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf     <- ATModule.>.id
            result <- ATModule.>.applyTf(tf, point)
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
        } yield (x, y, z, ATModule.>.translate(x, y, z), point)
      } {
        case (x, y, z, translation, point) =>
          unsafeRun(
            (for {
              transl          <- translation
              result          <- ATModule.>.applyTf(transl, point)
              resultCol       <- result |> toCol
              vectorToBeAdded <- Vec(x, y, z) |> toCol
              pointCol        <- point |> toCol
              expected        <- MatrixModule.>.add(vectorToBeAdded, pointCol)
              eq              <- MatrixModule.>.equal(expected, resultCol)
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
        } yield (ATModule.>.translate(x, y, z), vector)
      } {
        case (translation, vector) =>
          unsafeRun(
            (for {
              transl    <- translation
              result    <- ATModule.>.applyTf(transl, vector)
              resultCol <- result |> toCol
              vectorCol <- vector |> toCol
              eq        <- MatrixModule.>.equal(vectorCol, resultCol)
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
          x     <- reasonableDouble.filter(_ != 0d)
          y     <- reasonableDouble.filter(_ != 0d)
          z     <- reasonableDouble.filter(_ != 0d)
          point <- pointGen
        } yield (x, y, z, ATModule.>.scale(x, y, z), point)
      } {
        case (x, y, z, scaling, point) =>
          unsafeRun(
            (for {
              scal            <- scaling
              result          <- ATModule.>.applyTf(scal, point)
              resultCol       <- result |> toCol
              pointCol        <- point |> toCol
              vectorToBeAdded <- Pt(x, y, z) |> toCol
              expected        <- MatrixModule.>.had(vectorToBeAdded, pointCol)
              eq              <- MatrixModule.>.equal(expected, resultCol)
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
          rotateTf         <- ATModule.>.rotateZ(rotationAngle)
          scaleTf          <- ATModule.>.scale(20, 20, 20)
          translateTf      <- ATModule.>.translate(10, 30, 0)
          comp             <- ATModule.>.compose(rotateTf, scaleTf).flatMap(ATModule.>.compose(_, translateTf))
          res              <- ATModule.>.applyTf(comp, horizontalRadius)
          resCol           <- res |> toCol
          expectedCol      <- Pt(10, 50, 0) |> toCol
          _                <- MatrixModule.>.almostEqual(resCol, expectedCol, 10e-10).flatMap(res => IO.effect(res shouldEqual true))
        } yield ()).provide(env)
      }
    }

  }
}
