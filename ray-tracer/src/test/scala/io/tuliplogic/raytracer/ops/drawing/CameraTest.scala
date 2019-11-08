package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.data.{Camera, Ray}
import io.tuliplogic.raytracer.ops.model.modules.CameraModule
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, Task}

class CameraTest extends WordSpec with OpsTestUtils with DefaultRuntime {

  val env = new CameraModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule

  "A Camera" should {
    "compute the ray for pixel when the pixel is (halfWidth, halfHeight)" in {
      unsafeRun {
        (for {
          camera <- Camera.make(Pt.origin, Pt(0, 0, -1), Vec.uy, math.Pi / 2, 201, 101)
          ray    <- CameraModule.>.rayForPixel(camera, 100, 50)
          _      <- IO.effect(ray should ===(Ray(Pt.origin, Vec(0, 0, -1))))
        } yield ()).provide(env)
      }
    }

    "compute the ray for pixel when the pixel is (0, 0)" in {
      unsafeRun {
        (for {
          camera <- Camera.make(Pt.origin, Pt(0, 0, -1), Vec.uy, math.Pi / 2, 201, 101)
          ray    <- CameraModule.>.rayForPixel(camera, 0, 0)
          _      <- IO.effect(ray should ===(Ray(Pt.origin, Vec(0.66519, 0.33259, -0.66851))))
        } yield ()).provide(env)
      }
    }

    "compute the ray for pixel in (halfWidth, halfHeight) when the camera is transformed" in {
      unsafeRun {
        (for {
          camera <- Camera.make(Pt(0, 2, -5), Pt(0, 2, -5) + Vec(-math.sqrt(2) / 2, 0, -math.sqrt(2) / 2) , Vec(math.sqrt(2) / 2, math.sqrt(2) / 2, 0), math.Pi / 2, 201, 101)
          ray    <- CameraModule.>.rayForPixel(camera, 100, 50)
          _      <- IO.effect(ray should ===(Ray(Pt(0, 2, -5), Vec(-math.sqrt(2) / 2, 0, -math.sqrt(2) / 2))))
        } yield ()).provide(env)
      }
    }
  }
}
