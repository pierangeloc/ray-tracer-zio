package io.tuliplogic.raytracer.ops.model

import cats.data.NonEmptyList
import io.tuliplogic.raytracer.geometry.matrix.matrixOperations
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec._
import mouse.all._
import io.tuliplogic.raytracer.ops.model.RayOperations.Live
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.Sphere
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
                matrixOperations.equal(res1, exp1),
                matrixOperations.equal(res2, exp2),
                matrixOperations.equal(res3, exp3),
                matrixOperations.equal(res4, exp4)
              )
            )
            .flatMap(deltas => IO(deltas.forall(_ == true) shouldEqual true))
        } yield ()).provide(RayOperations.Live)
      }
    }

    "calculate intersection ray-unit sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
          s                  <- Sphere.unit
          intersectionPoints <- rayOps.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(4d, 6d).map(Intersection(_, s)))
        } yield ()).provide(Live)
      }
    }

    "calculate intersection ray-scaled sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        (for {
           tf <- AffineTransformation.scale(2, 2, 2)
           s                  <- UIO(Sphere(tf, Material.default))
           intersectionPoints <- rayOps.intersect(ray, s)
           _                  <- IO(intersectionPoints shouldEqual List(3d, 7d).map(Intersection(_, s)))
        } yield ()).provide(Live)
      }
    }

    "calculate hit from list of intersections" in {
      unsafeRun {
        (for {
          s   <- Sphere.unit
          intersections <- UIO.succeed(
            NonEmptyList.fromListUnsafe(List(Intersection(5, s), Intersection(7, s), Intersection(-3, s), Intersection(2, s)))
          )
          hit <- rayOps.hit(intersections)
          _   <- IO(hit.t shouldEqual 2)
          _   <- IO(hit.sceneObject shouldEqual s)
        } yield ()).provide(Live)
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
        } yield ()).provide(Live)
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
        } yield ()).provide(Live)
      }
    }
  }
}
