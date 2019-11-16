package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.TestUtils
import io.tuliplogic.raytracer.geometry.matrix.{Generators, MatrixModule, Types}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.ops.model.data.Camera
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO}

class ViewTransformTest extends WordSpec with DefaultRuntime with TestUtils with GeneratorDrivenPropertyChecks with Generators {

  val env = new ATModule.Live with MatrixModule.BreezeLive

  //TODO: property of this transform is that a view with proportional `to - from` vectors should give the same transformation

  "View transform" should {
    "provide identity transformation for default view" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf     <- Camera.canonicalTransformation
            result <- ATModule.>.applyTf(tf, point)
            _      <- IO.effect(result should ===(point))
          } yield ()).provide(env)
        )
      }
    }

    "provide mirror x and z axes view that points to positive z" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf     <- Camera.viewTransform(Pt.origin, Pt(0, 0, 1), Vec.uy)
            result <- ATModule.>.applyTf(tf, point)
            _      <- IO.effect(result should ===(point.copy(-point.x, point.y, -point.z)))
          } yield ()).provide(env)
        )
      }
    }

    "provide a translation of -deltaZ for any ViewTransform with origin in deltaZ" in {
      forAll {
        for {
          point  <- pointGen
          deltaZ <- reasonablePositiveDouble
        } yield (point, deltaZ)
      } {
        case (point, deltaZ) =>
          unsafeRun(
            (for {
              tf     <- Camera.viewTransform(Pt.origin.copy(z = deltaZ), Pt(0, 0, deltaZ - 1), Vec.uy)
              result <- ATModule.>.applyTf(tf, point)
              _      <- IO.effect(result should ===(point.copy(point.x, point.y, point.z - deltaZ)))
            } yield ()).provide(env)
          )
      }
    }

    "provide the correct transformation for this arbitrary ViewTransform" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf     <- Camera.viewTransform(Pt(1, 3, 2), Pt(4, -2, 8), Vec(1, 1, 0))
            result <- ATModule.>.applyTf(tf, point)
            expectedTfMatrix <- Types.factory.fromRows(
              4,
              4,
              Vector(
                Vector(-0.50709, 0.50709, 0.67612, -2.36643),
                Vector(0.76772, 0.60609, 0.12122, -2.82843),
                Vector(-0.35857, 0.59761, -0.71714, 0.00000),
                Vector(0.00000, 0.00000, 0.00000, 1.00000)
              )
            )
            inverseTfMatrix <- MatrixModule.>.invert(expectedTfMatrix)
            expected <- ATModule.>.applyTf(AT(expectedTfMatrix, inverseTfMatrix), point)
            _        <- IO.effect(result should ===(expected))
          } yield ()).provide(env)
        )

      }
    }
  }
}
