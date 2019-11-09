package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.OpsTestUtils.assertApproxEqual
import io.tuliplogic.raytracer.ops.model.data.Material
import io.tuliplogic.raytracer.ops.model.data.Scene.Sphere
import org.scalatest.Matchers._
import zio.test._
import zio.{Managed, UIO, ZIO}


object NormalReflectModuleSpec extends DefaultRunnableSpec(
  suite("NormalReflectModule")(
    testM("the normal vector to a point on the unit sphere should be the point minus origin")
    (for {
        s          <- Sphere.canonical
        pts        <- UIO.succeed(List(Pt(0, 1, 0), Pt(1, 0, 0), Pt(0, 0, 1)))
        normalVecs <- ZIO.sequence(pts.map(NormalReflectModule.>.normal(_, s)))
      } yield {
        val expected = pts.map(_ - Pt.origin)
        BoolAlgebra.collectAll(
          (normalVecs zip expected).map { case (v, e) => assertApproxEqual(v, e) }
        ).get
      }
    ),
    testM("the normal vector to a point on a transformed sphere should be computed as expected" ) {
      (for {
        tf <- ATModule.>.translate(0, 1, 0)
        mat <- Material.default
        s <- UIO.succeed(Sphere(tf, mat))
        normal <- NormalReflectModule.>.normal(Pt(0, 1.70711, -0.70711), s)
        _ <- ZIO.effect {
          normal === Vec(0, 0.70711, -0.70711)
        }
      } yield assertApproxEqual(normal, Vec(0, 0.70711, -0.70711)))
    }
  ).provideManagedShared(Managed.succeed(NormaReflectModuleSpecUtil.liveEnv))
)

object NormaReflectModuleSpecUtil {
  val liveEnv = new NormalReflectModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule
}
