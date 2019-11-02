package io.tuliplogic.raytracer

import io.tuliplogic.raytracer.geometry.TestUtils
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.model.SceneObject.Sphere
import io.tuliplogic.raytracer.ops.model.{Material, NormalReflectModule, spatialEntityOps}
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import zio.{DefaultRuntime, UIO, ZIO}

class NormalReflectModuleTest extends WordSpec with DefaultRuntime with TestUtils {

  val env = new NormalReflectModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule
  "the normal vector to a point on the unit sphere" should {
    "be the point minus origin" in {
      unsafeRun {
        (for {
          s          <- Sphere.unit
          pts        <- UIO.succeed(List(Pt(0, 1, 0), Pt(1, 0, 0), Pt(0, 0, 1)))
          normalVecs <- ZIO.sequence(pts.map(spatialEntityOps.normal(_, s)))
          _          <- ZIO.effect { normalVecs shouldEqual pts.map(_ - Pt(0, 0, 0)) }
        } yield ()).provide(env)
      }
    }
  }

  "the normal vector to a point on a transformed sphere" should {
    "be computed as expected" in {
      unsafeRun {
        (for {
          tf     <- ATModule.>.translate(0, 1, 0)
          mat    <- Material.default
          s      <- UIO.succeed(Sphere(tf, mat))
          normal <- spatialEntityOps.normal(Pt(0, 1.70711, -0.70711), s)
          _      <- ZIO.effect { normal === Vec(0, 0.70711, -0.70711) }
        } yield ()).provide(env)
      }
    }
  }
}
