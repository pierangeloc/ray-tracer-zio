package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.{CameraModule, Color, Ray}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, Task}

class CameraTest extends WordSpec with OpsTestUtils with DefaultRuntime {

  val env = new CameraModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule

  "A Camera" should {
    "compute the ray for pixel when the pixel is (halfWidth, halfHeight)" in {
      unsafeRun {
        (for {
          camera <- ATModule.>.id.map(Camera(201, 101, math.Pi / 2, _))
          ray    <- CameraModule.>.rayForPixel(camera, 100, 50)
          _      <- IO.effect(ray should ===(Ray(Pt.origin, Vec(0, 0, -1))))
        } yield ()).provide(env)
      }
    }

    "compute the ray for pixel when the pixel is (0, 0)" in {
      unsafeRun {
        (for {
          camera <- ATModule.>.id.map(Camera(201, 101, math.Pi / 2, _))
          ray    <- CameraModule.>.rayForPixel(camera, 0, 0)
          _      <- IO.effect(ray should ===(Ray(Pt.origin, Vec(0.66519, 0.33259, -0.66851))))
        } yield ()).provide(env)
      }
    }

    "compute the ray for pixel when the camera is transformed" in {
      unsafeRun {
        (for {
          rot    <- ATModule.>.rotateY(math.Pi / 4)
          transl <- ATModule.>.translate(0, -2, 5)
          camera <- (ATModule.>.compose(transl, rot)).map(Camera(201, 101, math.Pi / 2, _))
          ray    <- CameraModule.>.rayForPixel(camera, 100, 50)
          _      <- IO.effect(ray should ===(Ray(Pt(0, 2, -5), Vec(-math.sqrt(2) / 2, 0, -math.sqrt(2) / 2))))
        } yield ()).provide(env)
      }
    }
  }
}
