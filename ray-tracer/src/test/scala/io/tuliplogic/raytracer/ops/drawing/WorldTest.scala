package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.{AffineTransformation, AffineTransformationOps}
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.PhongReflection.HitComps
import io.tuliplogic.raytracer.ops.model.{Color, Intersection, Material, PhongReflection, Ray, RayOperations, SpatialEntityOperations}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{Plane, PointLight, Sphere}
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
          hc  <- World.hitComps(ray, i, List(i))
          _   <- IO { hc shouldEqual HitComps(s, Pt(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, -1)) }
        } yield ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live)
      }
    }

    "compute the components of a ray hitting a plane at 45 degrees, especially the rayReflectV" in {
      unsafeRun {
        (for {
          s   <- Plane.canonical
          ray <- UIO(Ray(Pt(0, 1, -1), Vec(0, -math.sqrt(2) / 2, math.sqrt(2) / 2)))
          i   <- UIO(Intersection(math.sqrt(2), s))
          hc  <- World.hitComps(ray, i, List(i))
          _ <- IO {
            hc should ===(HitComps(s, Pt.origin, Vec(0, 1, 0), Vec(0, math.sqrt(2) / 2, -math.sqrt(2) / 2), Vec(0, math.sqrt(2) / 2, math.sqrt(2) / 2)))
          }
        } yield ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live)
      }
    }

    "compute the n1, n2 correctly on all hits between transparent objects" in {
      unsafeRun {
        (for {
          w <- transparentSpheresWorld
          sA = w.objects(0)
          sB = w.objects(1)
          sC = w.objects(2)
          ray <- UIO(Ray(Pt(0, 0, -4), Vec.uz))
          is  <- UIO(
                  List(
                    Intersection(2, sA), Intersection(2.75, sB), Intersection(3.25, sC), Intersection(4.75, sB), Intersection(5.25, sC), Intersection(6, sA)
                  )
                )
          hc0  <- World.hitComps(ray, is(0), is)
          hc1  <- World.hitComps(ray, is(1), is)
          hc2  <- World.hitComps(ray, is(2), is)
          hc3  <- World.hitComps(ray, is(3), is)
          hc4  <- World.hitComps(ray, is(4), is)
          hc5  <- World.hitComps(ray, is(5), is)
          _ <- IO {
            (hc0.n1, hc0.n2) shouldEqual 1.0 -> 1.5
            (hc1.n1, hc1.n2) shouldEqual 1.5 -> 2.0
            (hc2.n1, hc2.n2) shouldEqual 2.0 -> 2.5
            (hc3.n1, hc3.n2) shouldEqual 2.5 -> 2.5
            (hc4.n1, hc4.n2) shouldEqual 2.5 -> 1.5
            (hc5.n1, hc5.n2) shouldEqual 1.5 -> 1.0
          }
        } yield ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live)
      }
    }

    "compute the underpoint" in {
      unsafeRun {
        (for {
          r <- UIO(Ray(Pt(0, 0, -5), Vec.uz))
          w <- oneTransparentCanonicalSphereWorld
          s <- UIO(w.objects.head)
          i <- UIO(Intersection(5, s))
          hc <- World.hitComps(r, i, List(i))
          _ <- IO{
            hc.underPoint.z > HitComps.epsilon / 2 shouldEqual true}
          _ <- IO{(hc.pt.z < hc.underPoint.z) shouldEqual true}
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
          _     <- IO { color should ===(Color(0.38066, 0.47583, 0.2855)) }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "compute correct color for a ray that intersects the innermost sphere, from the inside" ignore {
      unsafeRun {
        (for {
          w     <- defaultWorld
          ray   <- UIO(Ray(Pt(0, 0, 0.75), Vec(0, 0, -1)))
          color <- w.colorAt(ray)
          _     <- IO { color should ===(Color.white) }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "compute the correct color for a ray under shadow" in {
      unsafeRun {
        (for {
          w     <- defaultWorld
          ray   <- UIO(Ray(Pt(0, 0, 0.75), Vec(0, 0, -1)))
          color <- w.colorAt(ray)
          _     <- IO { color should ===(Color(0.1, 0.1, 0.1)) }
        } yield color)
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "compute the correct color as a sum of the color of the plane plus the color of the reflected sphere" in {
      unsafeRun {
        (for {
          w         <- defaultWorld4_1
          ray       <- UIO(Ray(Pt(0, 0, -3), Vec(0, -math.sqrt(2) / 2, math.sqrt(2) / 2)))
          plane     <- UIO(w.objects(2)) //TODO make a method to access objects by index in a World
          hit       <- UIO(Intersection(math.sqrt(2), plane))
          hitComps  <- World.hitComps(ray, hit, List(hit))
          fullColor <- w.colorAt(ray)
          _ <- IO {
            fullColor should ===(Color(0.87677, 0.92436, 0.82918))
          }
        } yield
          ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "terminate also in case of infinite reflection" in {
      unsafeRun {
        (for {
          w         <- defaultWorld5
          ray       <- UIO(Ray(Pt.origin, Vec(0, 1, 0)))
          fullColor <- w.colorAt(ray)
          _ <- IO {
            fullColor should not equal Color.black
          }
        } yield
          ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }
  }

  "isShadowed, with sphere of ray=1 around the origin and light at (-10, 10, -10)" should {
    "return false for a point in LOS with point light" in {
      unsafeRun {
        (for {
          w        <- defaultWorld
          inShadow <- w.isShadowed(Pt(0, 10, 0))
          _        <- IO { inShadow shouldEqual false }
        } yield ()).provide(RayOperations.Live)
      }
    }

    "return true for a point at the antipodes of the light, with the sphere being around the origin" in {
      unsafeRun {
        (for {
          w        <- defaultWorld
          inShadow <- w.isShadowed(Pt(10, -10, 10))
          _        <- IO { inShadow shouldEqual true }
        } yield ()).provide(RayOperations.Live)
      }
    }

    "return false for a point on the other side of the sphere light, with respect to the sphere" in {
      unsafeRun {
        (for {
          w        <- defaultWorld
          inShadow <- w.isShadowed(Pt(-20, 20, -20))
          _        <- IO { inShadow shouldEqual false }
        } yield ()).provide(RayOperations.Live)
      }
    }

    "return false for a point between the light and the sphere" in {
      unsafeRun {
        (for {
          w        <- defaultWorld
          inShadow <- w.isShadowed(Pt(-2, 2, -2))
          _        <- IO { inShadow shouldEqual false }
        } yield ()).provide(RayOperations.Live)
      }
    }
  }

  "color for a ray on a shadowed point" should {
    "equal the ambient light" in {
      unsafeRun {
        (for {
          w     <- defaultWorld3
          color <- w.colorAt(Ray(Pt(0, 0, 5), Vec(0, 0, 1)))
          _     <- IO { color should ===(Color(0.1, 0.1, 0.1)) }
        } yield
          ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with PhongReflection.Live with AffineTransformationOps.Live)
      }
    }
  }

  "reflected color" should {
    "return black for a material with reflective = 0 and ambient = 1" in {
      unsafeRun {
        (for {
          w              <- defaultWorld4
          ray            <- UIO(Ray(Pt(0, 0, 0), Vec(0, 0, 1)))
          s              <- UIO(w.objects(1))
          hit            <- UIO(Intersection(1, s))
          hitComps       <- World.hitComps(ray, hit, List(hit))
          reflectedColor <- World.reflectedColor(w, hitComps)
          _ <- IO {
            reflectedColor shouldEqual Color.black
          }
        } yield ())
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "return expected color for a material with reflective = 0.5 and default ambient" in {
      unsafeRun {
        (for {
          w              <- defaultWorld4_1
          ray            <- UIO(Ray(Pt(0, 0, -3), Vec(0, -math.sqrt(2) / 2, math.sqrt(2) / 2)))
          plane          <- UIO(w.objects(2)) //TODO make a method to access objects by index in a World
          hit            <- UIO(Intersection(math.sqrt(2), plane))
          hitComps       <- World.hitComps(ray, hit, List(hit))
          reflectedColor <- World.reflectedColor(w, hitComps)
          _ <- IO {
            reflectedColor should ===(Color(0.19032, 0.2379, 0.14274)) //it shoudl return these according to the book. they are actually proportional by 2.891
          }
        } yield
          ()).provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }
  }

  "refracted color" should {
    "be black for opaque surfaces" in {
      unsafeRun {
        (for {
          w              <- defaultWorld
          ray            <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          s              <- UIO(w.objects(0))
          intersections  <- UIO(List(Intersection(4, s), Intersection(6, s)))
          hitComps       <- World.hitComps(ray, intersections.head, intersections)
          refractedColor <- World.refractedColor(w, hitComps)
          _ <- IO {
            refractedColor shouldEqual Color.black
          }
        } yield ())
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "be black for excessive recursive depth" in {
      unsafeRun {
        (for {
          w              <- externalCanonicalTransparentInternalHalfOpaque
          ray            <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
          s              <- UIO(w.objects(0))
          intersections  <- UIO(List(Intersection(4, s), Intersection(6, s)))
          hitComps       <- World.hitComps(ray, intersections.head, intersections)
          refractedColor <- World.refractedColor(w, hitComps, 0)
          _ <- IO {
            refractedColor shouldEqual Color.black
            }
        } yield ())
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }

    "be black for total internal reflection" in {
      unsafeRun {
        (for {
          w              <- externalCanonicalTransparentInternalHalfOpaque
          ray            <- UIO(Ray(Pt(0, 0, math.sqrt(2) / 2), Vec(0, 1, 0)))
          s              <- UIO(w.objects(0))
          intersections  <- UIO(List(Intersection(- math.sqrt(2) / 2, s), Intersection(math.sqrt(2) / 2, s)))
          hitComps       <- World.hitComps(ray, intersections(1), intersections)
          refractedColor <- World.refractedColor(w, hitComps)
          _ <- IO {
            refractedColor shouldEqual Color.black
          }
        } yield ())
          .provide(new RayOperations.Live with SpatialEntityOperations.Live with MatrixOps.Live with AffineTransformationOps.Live with PhongReflection.Live)
      }
    }
  }
}

object WorldTest {
  val defaultWorld = for {
    pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    idTf <- AffineTransformation.id
    mat1 <- UIO(Material(Pattern.Uniform(Color(0.8, 1.0, 0.6), idTf), 0.1, 0.7, 0.2, 200, 0, 0, 1))
    tf1  <- AffineTransformation.id
    s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
    mat2 <- Material.default
    tf2  <- AffineTransformation.scale(0.5, 0.5, 0.5)
    s2   <- Sphere.withTransformAndMaterial(tf2, mat2)
    w    <- UIO(World(pl, List(s1, s2)))
  } yield w

  val defaultWorld2 = for {
    pl     <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    idTf   <- AffineTransformation.id
    defMat <- Material.default
    mat1   <- UIO(Material(Pattern.Uniform(Color(0.8, 1.0, 0.6), idTf), 1, 0.7, 0.2, 200, 0, 0, 1))
    tf1    <- AffineTransformation.id
    s1     <- Sphere.withTransformAndMaterial(tf1, mat1)
    mat2   <- UIO(defMat.copy(ambient = 1.0))
    tf2    <- AffineTransformation.scale(0.5, 0.5, 0.5)
    s2     <- Sphere.withTransformAndMaterial(tf2, mat2)
    w      <- UIO(World(pl, List(s1, s2)))
  } yield w

  val defaultWorld3 = for {
    pl   <- UIO(PointLight(Pt(0, 0, -10), Color.white))
    mat1 <- Material.default
    tf1  <- AffineTransformation.id
    s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
    tf2  <- AffineTransformation.translate(0, 0, 10)
    s2   <- Sphere.withTransformAndMaterial(tf2, mat1)
    w    <- UIO(World(pl, List(s1, s2)))
  } yield w

  val defaultWorld4 = for {
    pl       <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    idTf     <- AffineTransformation.id
    mat1     <- UIO(Material(Pattern.Uniform(Color(0.8, 1.0, 0.6), idTf), 1, 0.7, 0.2, 200, 0, 0, 1))
    tf1      <- AffineTransformation.id
    s1       <- Sphere.withTransformAndMaterial(tf1, mat1)
    mat2     <- Material.default
    tf2      <- AffineTransformation.scale(0.5, 0.5, 0.5)
    s2       <- Sphere.withTransformAndMaterial(tf2, mat2)
    planeTf  <- AffineTransformation.translate(0, -1, 0)
    planeMat <- Material.default.map(_.copy(reflective = 0.5))
    plane    <- UIO(Plane(planeTf, planeMat))
    w        <- UIO(World(pl, List(s1, s2, plane)))
  } yield w

  val defaultWorld4_1 = for {
    pl       <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    idTf     <- AffineTransformation.id
    mat1     <- UIO(Material(Pattern.Uniform(Color(0.8, 1.0, 0.6), idTf), 0.1, 0.7, 0.2, 200, 0, 0, 1))
    tf1      <- AffineTransformation.id
    s1       <- Sphere.withTransformAndMaterial(tf1, mat1)
    mat2     <- Material.default
    tf2      <- AffineTransformation.scale(0.5, 0.5, 0.5)
    s2       <- Sphere.withTransformAndMaterial(tf2, mat2)
    planeTf  <- AffineTransformation.translate(0, -1, 0)
    planeMat <- Material.default.map(_.copy(reflective = 0.5))
    plane    <- UIO(Plane(planeTf, planeMat))
    w        <- UIO(World(pl, List(s1, s2, plane)))
  } yield w

  val defaultWorld5 = for {
    pl     <- UIO(PointLight(Pt.origin, Color.white))
    mat    <- Material.default.map(_.copy(reflective = 0.5))
    tf1    <- AffineTransformation.translate(0, -1, 0)
    plane1 <- UIO(Plane(tf1, mat))
    tf2    <- AffineTransformation.translate(0, 1, 0)
    plane2 <- UIO(Plane(tf2, mat))
    w      <- UIO(World(pl, List(plane1, plane2)))
  } yield w

  val transparentSpheresWorld = for {
    pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    tf1  <- AffineTransformation.scale(2, 2, 2)
    s1   <- Sphere.unitGlass.map(s => s.copy(material = s.material.copy(refractionIndex = 1.5), transformation = tf1))
    tf2  <- AffineTransformation.translate(0, 0, -0.25)
    s2   <- Sphere.unitGlass.map(s => s.copy(material = s.material.copy(refractionIndex = 2), transformation = tf2))
    tf3  <- AffineTransformation.translate(0, 0, 0.25)
    s3   <- Sphere.unitGlass.map(s => s.copy(material = s.material.copy(refractionIndex = 2.5), transformation = tf3))
    w    <- UIO(World(pl, List(s1, s2, s3)))
  } yield w

  val oneTransparentCanonicalSphereWorld = for {
    pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
    tf  <- AffineTransformation.translate(0, 0, 1)
    s   <- Sphere.unitGlass.map(_.copy(transformation = tf))
    w    <- UIO(World(pl, List(s)))
  } yield w

  val externalCanonicalTransparentInternalHalfOpaque =
    for {
      pl   <- UIO(PointLight(Pt(-10, 10, -10), Color.white))
      idTf <- AffineTransformation.id
      mat1 <- UIO(Material(Pattern.Uniform(Color(0.8, 1.0, 0.6), idTf), 0.1, 0.7, 0.2, 200, 0, 1, 1.5))
      tf1  <- AffineTransformation.id
      s1   <- Sphere.withTransformAndMaterial(tf1, mat1)
      mat2 <- Material.default
      tf2  <- AffineTransformation.scale(0.5, 0.5, 0.5)
      s2   <- Sphere.withTransformAndMaterial(tf2, mat2)
      w    <- UIO(World(pl, List(s1, s2)))
    } yield w

}
