package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.{AffineTransformation, AffineTransformationOps}
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.PhongReflection.HitComps
import io.tuliplogic.raytracer.ops.model.{Color, Intersection, Material, PhongReflection, Ray, RayOperations, SpatialEntityOperations}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO}

class WorldTest extends WordSpec with DefaultRuntime with OpsTestUtils {
  import WorldTest._

  "intersect" should {
    "compute intersections of a ray with all the objects of the world" in {
      unsafeRun {
        (for {
          w   <- defaultWorld
          ray <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          xs  <- w.intersect(ray)
          _   <- IO { xs.map(_.t) shouldEqual List(4.0, 4.5, 5.5, 6.0) }
        } yield ()).provide(RayOperations.Live)
      }
    }
  }

  "hitComps" should {
    "compute the components of a ray hitting the surface at a given intersection" in {
      unsafeRun {
        (for {
          s   <- Sphere.unit
          i   <- UIO(Intersection(4, s))
          ray <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          hc  <- World.hitComps(ray, i)
          _   <- IO { hc shouldEqual HitComps(s, Pt(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, -1)) }
        } yield ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live)
      }
    }
  }

  "colorAt" should {
    "compute black for a ray that doesn't intersect any object" in {
      unsafeRun {
        (for {
          w     <- defaultWorld
          ray   <- UIO(Ray(Pt(0, 0, -5), Vec(0, 1, 0)))
          color <- w.colorAt(ray)
          _     <- IO { color shouldEqual Color.black }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "compute correct color for a ray that intersects the outmost sphere" in {
      unsafeRun {
        (for {
          w     <- defaultWorld
          ray   <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          color <- w.colorAt(ray)
          _     <- IO { color === Color(0.38066, 0.47583, 0.2855) }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "compute correct color for a ray that intersects the innermost sphere, from the inside" in {
      unsafeRun {
        (for {
          w     <- defaultWorld2
          ray   <- UIO(Ray(Pt(0, 0, 0.75), Vec(0, 0, -1)))
          color <- w.colorAt(ray)
          _     <- IO { color === Color.white }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }
  }
}

object WorldTest {
  val defaultWorld = for {
    pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
      mat1 <- UIO(Material(Color(0.8, 1.0, 0.6), 0.1, 0.7, 0.2, 200))
      tf1  <- AffineTransformation.id
      s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
      mat2 <- UIO(Material.default)
      tf2  <- AffineTransformation.scale(0.5, 0.5, 0.5)
      s2   <- Sphere.withTransformAndMaterial(tf2, mat2)
      w    <- UIO(World(pl, List(s1, s2)))
  } yield w

  val defaultWorld2 = for {
    pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
      mat1 <- UIO(Material(Color(0.8, 1.0, 0.6), 1, 0.7, 0.2, 200))
      tf1  <- AffineTransformation.id
      s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
      mat2 <- UIO(Material.default.copy(ambient = 1.0))
      tf2  <- AffineTransformation.scale(0.5, 0.5, 0.5)
      s2   <- Sphere.withTransformAndMaterial(tf2, mat2)
      w    <- UIO(World(pl, List(s1, s2)))
  } yield w
}