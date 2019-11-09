package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.drawing.WorldTest.defaultWorld
import io.tuliplogic.raytracer.ops.model.data.{Ray, RayModule}
import zio.{Managed, UIO}
import zio.test._
import zio.test.Assertion._

object WorldTopologyModuleSpec
    extends DefaultRunnableSpec(
      suite("WorldTopologyModule.Live") (
        testM("intersect should compute intersections of a ray with all the objects of the world") {
          for {
            w   <- defaultWorld
            ray <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
            xs  <- WorldTopologyModule.>.intersections(w, ray)
          } yield assert(xs.map(_.t), equalTo(List(4.0, 4.5, 5.5, 6.0)))
        },
        suite("isShadowed, with sphere of ray=1 around the origin and light at (-10, 10, -10)") (
          testM("return false for a point in LOS with point light") {
            for {
              w        <- defaultWorld
                inShadow <- WorldTopologyModule.>.isShadowed(w, Pt(0, 10, 0))
            } yield assert(inShadow, equalTo(false))
          },
          testM("return false for a point in LOS with point light") {
            for {
              w        <- defaultWorld
                inShadow <- WorldTopologyModule.>.isShadowed(w, Pt(10, -10, 10))
            } yield assert(inShadow, equalTo(true))
          },
          testM("return false for a point on the other side of the sphere light, with respect to the sphere") {
            for {
              w        <- defaultWorld
              inShadow <- WorldTopologyModule.>.isShadowed(w, Pt(-20, 20, -20))
            } yield assert(inShadow, equalTo(false))
          },
          testM("return false for a point between the light and the sphere") {
            for {
              w        <- defaultWorld
                inShadow <- WorldTopologyModule.>.isShadowed(w, Pt(-2, 2, -2))
            } yield assert(inShadow, equalTo(false))
          }
        )
    ).provideManagedShared(Managed.succeed(WorldTopologyModuleSpecUtil.liveEnv))
  )

object WorldTopologyModuleSpecUtil {
  val liveEnv = new WorldTopologyModule.Live with RayModule.Live with ATModule.Live with MatrixModule.BreezeMatrixModule

}