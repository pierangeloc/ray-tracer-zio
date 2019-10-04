package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.TestUtils
import io.tuliplogic.raytracer.geometry.matrix.{Generators, MatrixOps, Types}
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps.Live
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.vectorspace.{affineTfOps, AffineTransformation}
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import zio.{DefaultRuntime, IO}

class ViewTransformTest extends WordSpec with DefaultRuntime with TestUtils with GeneratorDrivenPropertyChecks with Generators {

  val env = new Live with MatrixOps.Live

  //TODO: property of this transform is that a view with proportional `to - from` vectors should give the same transformation

  "View transform" should {
    "provide identity transformaton for default view" in {
      forAll {
        pointGen
      } { point =>
        unsafeRun(
          (for {
            tf <- ViewTransform.default.tf
            result <- affineTfOps.transform(tf, point)
            _      <- IO.effect(result === point)
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
            tf <- ViewTransform(Pt.origin, Pt(0, 0, 1), Vec.uy).tf
            result <- affineTfOps.transform(tf, point)
            _      <- IO.effect(result === point.copy(-point.x, point.y, -point.z))
          } yield ()).provide(env)
        )
      }
    }

    "provide a translation of -deltaZ for any ViewTransform with origini in deltaZ" in {
      forAll {
        for {
          pt     <- pointGen
          deltaZ <- reasonableDouble
        } yield (pt, deltaZ)
      } {
        case (point, deltaZ) =>
          unsafeRun(
            (for {
              tf <- ViewTransform(Pt.origin.copy(z = deltaZ), Pt(0, 0, 1), Vec.uy).tf
              result <- affineTfOps.transform(tf, point)
              _      <- IO.effect(result === point.copy(point.x, point.y, point.z - deltaZ))
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
            tf <- ViewTransform(Pt(1, 3, 2), Pt(4, -2, 8), Vec(1, 1, 0)).tf
            result <- affineTfOps.transform(tf, point)
            expectedTfMatrix <- Types.factory.fromRows(4, 4,
              Vector(
                Vector(-0.50709, 0.50709, 0.67612, -2.36643),
                Vector(0.76772, 0.60609, 0.12122, -2.82843),
                Vector(-0.35857, 0.59761, -0.71714, 0.00000),
                Vector(0.00000, 0.00000, 0.00000, 1.00000)
              )
            )
            expected <- affineTfOps.transform(AffineTransformation(expectedTfMatrix), point)
            _        <- IO.effect(result === expected)
          } yield ()).provide(env)
        )

      }
    }
  }
}
