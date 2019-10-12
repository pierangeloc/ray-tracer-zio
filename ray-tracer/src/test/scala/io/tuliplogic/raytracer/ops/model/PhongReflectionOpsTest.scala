package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.drawing.Pattern
import io.tuliplogic.raytracer.ops.model.PhongReflection.{HitComps, PhongComponents}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO}

class PhongReflectionOpsTest extends WordSpec with DefaultRuntime with OpsTestUtils {
  "phong reflection model live" should {
    "give correct phong components when eye is in LOS with source and aligned with normal" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, Pt(0, 0, 0), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 0, -10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, false)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.white * 0.9, Color.white * 0.9)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye is 45 deg off the normal and light aligned with normal" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, Pt(0, 0, 0), eyeV = Vec(0, math.sqrt(2) / 2, -math.sqrt(2) / 2), normalV = Vec(0, 0, -1), rayReflectV = Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 0, -10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, false)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.white * 0.9, Color.black)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye aligned with normal and light is 45 deg off" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, pt = Pt(0, 0, 0), normalV = Vec(0, 0, -1), eyeV = Vec(0, 0, -1), rayReflectV = Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 10, -10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, false)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.white * (0.9 * math.sqrt(2) / 2), Color.black)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye aligned with reflection vector light is 45 deg off" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, Pt(0, 0, 0), Vec(0, 0, -1), Vec(0, -math.sqrt(2) / 2, -math.sqrt(2) / 2), Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 10, -10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, false)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.white * (0.9 * math.sqrt(2) / 2), Color.white * 0.9)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye is on the other side of the light, aligned with normal" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, Pt(0, 0, 0), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 0, 10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, false)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.black, Color.black)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye is in LOS with source and aligned with normal, but shadowed" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          hitComps   <- UIO(HitComps(s, Pt(0, 0, 0), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, 0)))
          pointLight <- UIO(PointLight(Pt(0, 0, -10), Color.white))
          res        <- phongOps.lighting(pointLight, hitComps, true)
          _          <- IO(res should ===(PhongComponents(Color.white * 0.1, Color.black, Color.black)))
        } yield res).provide(PhongReflection.Live)
      }
    }

    "give correct phong components when eye in LOS with source and material is striped" in {
      unsafeRun {
        (for {
          tf         <- AffineTransformation.id
          mat        <- UIO(Material(pattern = Pattern.Striped(Color.white, Color.black, tf), ambient = 1, diffuse = 0, specular = 0, shininess = 0, reflective = 0))
          s          <- Sphere.withTransformAndMaterial(tf, mat)
          pointLight <- UIO(PointLight(Pt(0, 0, -10), Color.white))
          hitComps1  <- UIO(HitComps(s, Pt(0.9, 0, 0), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, 0)))
          res1       <- phongOps.lighting(pointLight, hitComps1, false)
          _          <- IO(res1.toColor should ===(Color.white))
          hitComps2  <- UIO(HitComps(s, Pt(1.1, 0, 0), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, 0)))
          res2       <- phongOps.lighting(pointLight, hitComps2, false)
          _          <- IO(res2.toColor should ===(Color.black))
        } yield res1).provide(PhongReflection.Live)
      }
    }
  }
}
