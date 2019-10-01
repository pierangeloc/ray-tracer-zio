package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.{Color, Material, Ray, RayOperations}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO}

class WorldTest extends WordSpec with DefaultRuntime with OpsTestUtils {

  "intersect" should {
    "compute intersections of a ray with all the objects of the world" in {
      unsafeRun{
        (for {
          pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
          mat1 <- UIO(Material(Color(0.8, 1.0, 0.6), 0.1, 0.7, 0.2, 200))
          tf1  <- AffineTransformation.id
          s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
          mat2 <- UIO(Material.default)
          tf2  <- AffineTransformation.scale(0.5, 0.5, 0.5)
          s2   <- Sphere.withTransformAndMaterial(tf2, mat2)
          w    <- UIO(World(pl, List(s1, s2)))
          ray  <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          xs   <- w.intersect(ray)
          _    <- IO {xs.map(_.t) shouldEqual List(4.0, 4.5, 5.5, 6.0)}
        } yield ()).provide(RayOperations.Live)
      }
    }
  }
}
