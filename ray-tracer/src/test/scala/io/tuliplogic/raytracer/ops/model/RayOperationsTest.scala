package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.matrix.>
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec._
import mouse.all._
import io.tuliplogic.raytracer.ops.model.RayOperations.BreezeMatrixOps$
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{Plane, Sphere}
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, UIO, ZIO}

class RayOperationsTest extends WordSpec with DefaultRuntime {

  "Ray operations" should {
    "calculate position at different t" in {
      unsafeRun {
        val p   = Pt(2, 3, 4)
        val v   = Vec(1, 0, 0)
        val ray = Ray(p, v)

        (for {
          res1 <- rayOps.positionAt(ray, 0) >>= toCol
          res2 <- rayOps.positionAt(ray, 1) >>= toCol
          res3 <- rayOps.positionAt(ray, -1) >>= toCol
          res4 <- rayOps.positionAt(ray, 2.5) >>= toCol
          exp1 <- Pt(2, 3, 4) |> toCol
          exp2 <- Pt(3, 3, 4) |> toCol
          exp3 <- Pt(1, 3, 4) |> toCol
          exp4 <- Pt(4.5, 3, 4) |> toCol
          _ <- ZIO
            .sequence(
              List(
                >.equal(res1, exp1),
                >.equal(res2, exp2),
                >.equal(res3, exp3),
                >.equal(res4, exp4)
              )
            )
            .flatMap(deltas => IO(deltas.forall(_ == true) shouldEqual true))
        } yield ()).provide(RayOperations.BreezeMatrixOps$)
      }
    }

    "calculate intersection ray-unit sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
          s                  <- Sphere.unit
          intersectionPoints <- rayOps.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(4d, 6d).map(Intersection(_, s)))
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "calculate (empty) intersections ray-horizontal plane with a ray parallel to the plane" in {
      unsafeRun {
        val ray = Ray(Pt(0, 10, 0), Vec(0, 0, 1))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- rayOps.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List())
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "calculate (empty) intersections ray-horizontal plane with a ray coplanar with the plane" in {
      unsafeRun {
        val ray = Ray(Pt.origin, Vec(0, 0, 1))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- rayOps.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List())
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "calculate intersection ray-horizontal plane" in {
      unsafeRun {
        val ray = Ray(Pt(0, 1, 0), Vec(0, -1, 0))
        (for {
          p                  <- Plane.canonical
          intersectionPoints <- rayOps.intersect(ray, p)
          _                  <- IO(intersectionPoints shouldEqual List(Intersection(1, p)))
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "calculate intersection ray-scaled sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
          tf                 <- AffineTransformation.scale(2, 2, 2)
          mat                <- Material.default
          s                  <- UIO(Sphere(tf, mat))
          intersectionPoints <- rayOps.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(3d, 7d).map(Intersection(_, s)))
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "calculate hit from list of intersections" in {
      unsafeRun {
        (for {
          s             <- Sphere.unit
          intersections <- UIO.succeed(List(Intersection(5, s), Intersection(7, s), Intersection(-3, s), Intersection(2, s)))
          hit           <- rayOps.hit(intersections)
          _             <- IO(hit.get.t shouldEqual 2)
          _             <- IO(hit.get.sceneObject shouldEqual s)
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "perform ray translation translating origin and leaving direction as is" in {
      unsafeRun {
        val ray = Ray(Pt(1, 2, 3), Vec(0, 1, 0))

        (for {
          tf  <- AffineTransformation.translate(3, 4, 5)
          res <- rayOps.transform(tf, ray)
          _   <- IO(res.origin shouldEqual Pt(4, 6, 8))
          _   <- IO(res.direction shouldEqual Vec(0, 1, 0))
        } yield ()).provide(BreezeMatrixOps$)
      }
    }

    "perform ray scaling scaling origin and scaling direction as is" in {
      unsafeRun {
        val ray = Ray(Pt(1, 2, 3), Vec(0, 1, 0))

        (for {
          tf  <- AffineTransformation.scale(2, 3, 4)
          res <- rayOps.transform(tf, ray)
          _   <- IO(res.origin shouldEqual Pt(2, 6, 12))
          _   <- IO(res.direction shouldEqual Vec(0, 3, 0))
        } yield ()).provide(BreezeMatrixOps$)
      }
    }
  }
}
