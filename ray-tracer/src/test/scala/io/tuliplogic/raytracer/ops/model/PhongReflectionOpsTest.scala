package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.matrix.Types
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.PhongReflection.PhongComponents
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.PointLight
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO}


class PhongReflectionOpsTest extends WordSpec with DefaultRuntime with OpsTestUtils {
  "phong reflection model live" should {
    "give correct phong components when eye is in LOS with source and aligned with normal" in {
      unsafeRun {
        (for {
          surfacePt    <- UIO(Pt(0, 0, 0))
          eyeVector    <- UIO(Vec(0, 0, -1))
          normalVector <- UIO(Vec(0, 0, -1))
          pointLight   <- UIO(PointLight(Pt(0, 0, -10), 1))
          res          <- phong.lighting(Material.default, pointLight, surfacePt, eyeVector, normalVector)
          _            <- IO(res === PhongComponents(Color.white * 0.1, Color.white * 0.9, Color.white * 0.9))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye is 45 deg off the normal and light aligned with normal" in {
      unsafeRun {
        (for {
          surfacePt    <- UIO(Pt(0, 0, 0))
          eyeVector    <- UIO(Vec(0, math.sqrt(2) / 2, -math.sqrt(2) / 2))
          normalVector <- UIO(Vec(0, 0, -1))
          pointLight   <- UIO(PointLight(Pt(0, 0, -10), 1))
          res          <- phong.lighting(Material.default, pointLight, surfacePt, eyeVector, normalVector)
          _            <- IO(res === PhongComponents(Color.white * 0.1, Color.white * 0.9, Color.black))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye aligned with normal and light is 45 deg off" in {
      unsafeRun {
        (for {
          surfacePt    <- UIO(Pt(0, 0, 0))
          eyeVector    <- UIO(Vec(0, 0, -1))
          normalVector <- UIO(Vec(0, 0, -1))
          pointLight   <- UIO(PointLight(Pt(0, 10, -10), 1))
          res          <- phong.lighting(Material.default, pointLight, surfacePt, eyeVector, normalVector)
          _            <- IO(res === PhongComponents(Color.white * 0.1, Color.white *(0.9 * math.sqrt(2) / 2), Color.black))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye aligned with reflection vector light is 45 deg off" in {
      unsafeRun {
        (for {
          surfacePt    <- UIO(Pt(0, 0, 0))
            eyeVector    <- UIO(Vec(0, -math.sqrt(2) / 2, -math.sqrt(2) / 2))
            normalVector <- UIO(Vec(0, 0, -1))
            pointLight   <- UIO(PointLight(Pt(0, 10, -10), 1))
            res          <- phong.lighting(Material.default, pointLight, surfacePt, eyeVector, normalVector)
            _            <- IO(res === PhongComponents(Color.white * 0.1, Color.white *(0.9 * math.sqrt(2) / 2), Color.white * 0.9))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye is on the other side of the light, aligned with normal" in {
      unsafeRun {
        (for {
          surfacePt    <- UIO(Pt(0, 0, 0))
            eyeVector    <- UIO(Vec(0, 0, -1))
            normalVector <- UIO(Vec(0, 0, -1))
            pointLight   <- UIO(PointLight(Pt(0, 0, 10), 1))
            res          <- phong.lighting(Material.default, pointLight, surfacePt, eyeVector, normalVector)
            _            <- IO(res === PhongComponents(Color.white * 0.1, Color.black, Color.black))
        } yield res).provide(PhongReflection.Live)
      }
    }
  }
}
