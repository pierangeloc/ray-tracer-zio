package io.tuliplogic.raytracer.model

import io.tuliplogic.geometry.matrix._
import io.tuliplogic.raytracer.model.RayOps.MatrixOpsRayService
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{DefaultRuntime, IO, ZIO}


class RayOpsTest extends WordSpec with DefaultRuntime {
  import io.tuliplogic.geometry.matrix.AffineTransformation.{point, vector}

  "Ray operations" should {
    "calculate position at different t" in {
      unsafeRun {
        (for {
          p <- point(2, 3, 4)
          v <- vector(1, 0, 0)
          ray = Ray(p, v)
          res1 <- rayOperations.positionAt(ray, 0)
          res2 <- rayOperations.positionAt(ray, 1)
          res3 <- rayOperations.positionAt(ray, -1)
          res4 <- rayOperations.positionAt(ray, 2.5)
          exp1 <- point(2, 3, 4)
          exp2 <- point(3, 3, 4)
          exp3 <- point(1, 3, 4)
          exp4 <- point(4.5, 3, 4)
          _    <- ZIO.sequence(
            List(
              matrixOperations.equal(res1.col, exp1.col),
              matrixOperations.equal(res2.col, exp2.col),
              matrixOperations.equal(res3.col, exp3.col),
              matrixOperations.equal(res4.col, exp4.col)
            )
          ).flatMap(deltas => IO(deltas.forall(_ == true) shouldEqual true))
        } yield ()).provide(new MatrixOpsRayService with MatrixOps.LiveMatrixOps {})
      }
    }
  }

}
