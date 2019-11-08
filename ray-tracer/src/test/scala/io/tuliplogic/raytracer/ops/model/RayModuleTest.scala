package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import mouse.all._
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Intersection, Material, Ray, RayModule}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO, ZIO}

class RayModuleTest extends WordSpec with DefaultRuntime {

  "Ray operations" should {
    "calculate position at different t" in {
      unsafeRun {
        val p   = Pt(2, 3, 4)
        val v   = Vec(1, 0, 0)
        val ray = Ray(p, v)

        (for {
          res1 <- toCol(ray.positionAt(0))
          res2 <- toCol(ray.positionAt(1))
          res3 <- toCol(ray.positionAt( -1))
          res4 <- toCol(ray.positionAt(2.5))
          exp1 <- Pt(2, 3, 4) |> toCol
          exp2 <- Pt(3, 3, 4) |> toCol
          exp3 <- Pt(1, 3, 4) |> toCol
          exp4 <- Pt(4.5, 3, 4) |> toCol
          _ <- ZIO
            .sequence(
              List(
                MatrixModule.>.equal(res1, exp1),
                MatrixModule.>.equal(res2, exp2),
                MatrixModule.>.equal(res3, exp3),
                MatrixModule.>.equal(res4, exp4)
              )
            )
            .flatMap(deltas => IO(deltas.forall(_ == true) shouldEqual true))
        } yield ()).provide(new MatrixModule.BreezeMatrixModule{})
      }
    }

    val rayEnv = new RayModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule
    "calculate intersection ray-unit sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
          s                  <- Sphere.canonical
          intersectionPoints <- RayModule.>.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(4d, 6d).map(Intersection(_, s)))
        } yield ()).provide(rayEnv)
      }
    }

    "calculate (empty) intersections ray-horizontal plane with a ray parallel to the plane" in {
      unsafeRun {
        val ray = Ray(Pt(0, 10, 0), Vec(0, 0, 1))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- RayModule.>.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List())
        } yield ()).provide(rayEnv)
      }
    }

    "calculate (empty) intersections ray-horizontal plane with a ray coplanar with the plane" in {
      unsafeRun {
        val ray = Ray(Pt.origin, Vec(0, 0, 1))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- RayModule.>.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List())
        } yield ()).provide(rayEnv)
      }
    }

    "calculate intersection ray-horizontal plane" in {
      unsafeRun {
        val ray = Ray(Pt(0, 1, 0), Vec(0, -1, 0))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- RayModule.>.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List(data.Intersection(1, p)))
        } yield ()).provide(rayEnv)
      }
    }

    "calculate intersection ray-scaled sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
          tf                 <- ATModule.>.scale(2, 2, 2)
          mat                <- Material.default
          s                  <- UIO(Sphere(tf, mat))
          intersectionPoints <- RayModule.>.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(3d, 7d).map(data.Intersection(_, s)))
        } yield ()).provide(rayEnv)
      }
    }

    "calculate hit from list of intersections" in {
      unsafeRun {
        (for {
          s             <- Sphere.canonical
          intersections <- UIO.succeed(List(data.Intersection(5, s), data.Intersection(7, s), data.Intersection(-3, s), data.Intersection(2, s)))
          hit           <- RayModule.>.hit(intersections)
          _             <- IO(hit.get.t shouldEqual 2)
          _             <- IO(hit.get.sceneObject shouldEqual s)
        } yield ()).provide(rayEnv)
      }
    }

    "perform ray translation translating origin and leaving direction as is" in {
      unsafeRun {
        val ray = Ray(Pt(1, 2, 3), Vec(0, 1, 0))

        (for {
          tf  <- ATModule.>.translate(3, 4, 5)
          res <- RayModule.>.transform(tf, ray)
          _   <- IO(res.origin shouldEqual Pt(4, 6, 8))
          _   <- IO(res.direction shouldEqual Vec(0, 1, 0))
        } yield ()).provide(rayEnv)
      }
    }

    "perform ray scaling scaling origin and scaling direction as is" in {
      unsafeRun {
        val ray = Ray(Pt(1, 2, 3), Vec(0, 1, 0))

        (for {
          tf  <- ATModule.>.scale(2, 3, 4)
          res <- RayModule.>.transform(tf, ray)
          _   <- IO(res.origin shouldEqual Pt(2, 6, 12))
          _   <- IO(res.direction shouldEqual Vec(0, 3, 0))
        } yield ()).provide(rayEnv)
      }
    }
  }
}
