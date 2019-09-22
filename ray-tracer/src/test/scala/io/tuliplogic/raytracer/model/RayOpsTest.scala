package io.tuliplogic.raytracer.model

import io.tuliplogic.geometry.matrix.SpatialEntity.{Pt, Vec, toCol}
import mouse.all._
import io.tuliplogic.geometry.matrix._
import io.tuliplogic.raytracer.model.RayOps.MatrixOpsRayService
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, ZIO}


class RayOpsTest extends WordSpec with DefaultRuntime {

  "Ray operations" should {
    "calculate position at different t" in {
      unsafeRun {
        val p = Pt(2, 3, 4)
        val v = Vec(1, 0, 0)
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
          _    <- ZIO.sequence(
            List(
              matrixOperations.equal(res1, exp1),
              matrixOperations.equal(res2, exp2),
              matrixOperations.equal(res3, exp3),
              matrixOperations.equal(res4, exp4)
            )
          ).flatMap(deltas => IO(deltas.forall(_ == true) shouldEqual true))
        } yield ()).provide(new MatrixOpsRayService with MatrixOps.LiveMatrixOps {})
      }
    }
  }

}
