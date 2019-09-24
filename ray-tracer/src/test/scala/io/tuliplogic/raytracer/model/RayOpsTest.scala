package io.tuliplogic.raytracer.model

import cats.data.NonEmptyList
import io.tuliplogic.geometry.matrix.SpatialEntity.SceneObject._
import io.tuliplogic.geometry.matrix.SpatialEntity.{toCol, Pt, Vec}
import mouse.all._
import io.tuliplogic.geometry.matrix._
import io.tuliplogic.raytracer.model.RayOps.Live
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, ZIO}

class RayOpsTest extends WordSpec with DefaultRuntime {

  "Ray operations" should {
    "calculate position at different t" in {
      unsafeRun {
        val p   = Pt(2, 3, 4)
        val v   = Vec(1, 0, 0)
        val ray = Ray(p, v)

        (for {
          res1 <- rayOperations.positionAt(ray, 0) >>= toCol
          res2 <- rayOperations.positionAt(ray, 1) >>= toCol
          res3 <- rayOperations.positionAt(ray, -1) >>= toCol
          res4 <- rayOperations.positionAt(ray, 2.5) >>= toCol
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
        } yield ()).provide(RayOps.Live)
      }
    }

    "calculate intersection ray-sphere" in {
      unsafeRun {
        val ray = Ray(Pt(0, 0, -5), Vec(0, 0, 1))
        val s   = Sphere(Pt(0, 0, 0), 1)
        (for {
          intersectionPoints <- rayOperations.intersect(ray, s)
          _                  <- IO(intersectionPoints shouldEqual List(4d, 6d).map(Intersection(_, s)))
        } yield ()).provide(Live)
      }
    }

    "calculate hit" in {
      unsafeRun {
        val s             = Sphere(Pt(0, 0, 0), 1)
        val intersections = NonEmptyList.fromListUnsafe(List(Intersection(5, s), Intersection(7, s), Intersection(-3, s), Intersection(2, s)))
        (for {
          hit <- rayOperations.hit(intersections)
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
          res <- rayOperations.transform(tf, ray)
          _ <- IO(res.origin shouldEqual Pt(4, 6, 8))
            _ <- IO(res.direction shouldEqual Vec(0, 1, 0))
        } yield ()).provide(Live)
      }
    }

    "perform ray scaling scaling origin and scaling direction as is" in {
      unsafeRun {
        val ray = Ray(Pt(1, 2, 3), Vec(0, 1, 0))

        (for {
          tf  <- AffineTransformation.scale(2, 3, 4)
            res <- rayOperations.transform(tf, ray)
            _ <- IO(res.origin shouldEqual Pt(2, 6, 12))
            _ <- IO(res.direction shouldEqual Vec(0, 3, 0))
        } yield ()).provide(Live)
      }
    }
  }
}
