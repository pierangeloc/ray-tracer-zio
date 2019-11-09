package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.{LiveFullATModule, OpsTestUtils}
import io.tuliplogic.raytracer.ops.drawing.WorldTest
import io.tuliplogic.raytracer.ops.model.data.{Color, Intersection, Ray}
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, Sphere}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.HitComps
import zio.{Managed, UIO}
import zio.test._
import zio.test.Assertion._
import org.scalactic.TripleEquals._
import OpsTestUtils._

object WorldHitCompsModuleSpec extends DefaultRunnableSpec(
  suite("WorldHitCompsModule.Live")(
    testM("compute the components of a ray hitting the surface at a given intersection") (
      for {
        s   <- Sphere.canonical
        i   <- UIO(Intersection(4, s))
        ray <- UIO(Ray(Pt(0, 0, -5), Vec(0, 0, 1)))
        hc  <- WorldHitCompsModule.>.hitComps(ray, i, List(i))
      } yield assertApproxEqual(hc, HitComps(s, Pt(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, -1), Vec(0, 0, -1)))
    ),
    testM("compute the components of a ray hitting the surface at a given intersection") (
      for {
        s <- Plane.canonical
          ray <- UIO(Ray(Pt(0, 1, -1), Vec(0, -math.sqrt(2) / 2, math.sqrt(2) / 2)))
          i <- UIO(Intersection(math.sqrt(2), s))
          hc <- WorldHitCompsModule.>.hitComps(ray, i, List(i))
      } yield assertApproxEqual(hc, HitComps(s, Pt.origin, Vec(0, 1, 0), Vec(0, math.sqrt(2) / 2, -math.sqrt(2) / 2), Vec(0, math.sqrt(2) / 2, math.sqrt(2) / 2)))
    ),
    testM("compute the n1, n2 correctly on all hits between transparent objects")(
      for {
        w <- WorldTest.transparentSpheresWorld
        sA = w.objects(0)
        sB = w.objects(1)
        sC = w.objects(2)
        ray <- UIO(Ray(Pt(0, 0, -4), Vec.uz))
        is <- UIO(
          List(
            Intersection(2, sA), Intersection(2.75, sB), Intersection(3.25, sC), Intersection(4.75, sB), Intersection(5.25, sC), Intersection(6, sA)
          )
        )
        hc0 <- WorldHitCompsModule.>.hitComps(ray, is(0), is)
        hc1 <- WorldHitCompsModule.>.hitComps(ray, is(1), is)
        hc2 <- WorldHitCompsModule.>.hitComps(ray, is(2), is)
        hc3 <- WorldHitCompsModule.>.hitComps(ray, is(3), is)
        hc4 <- WorldHitCompsModule.>.hitComps(ray, is(4), is)
        hc5 <- WorldHitCompsModule.>.hitComps(ray, is(5), is)
      } yield assert((hc0.n1, hc0.n2) === 1.0 -> 1.5, equalTo(true)) &&
              assert((hc1.n1, hc1.n2) === 1.5 -> 2.0, equalTo(true)) &&
              assert((hc2.n1, hc2.n2) === 2.0 -> 2.5, equalTo(true)) &&
              assert((hc3.n1, hc3.n2) === 2.5 -> 2.5, equalTo(true)) &&
              assert((hc4.n1, hc4.n2) === 2.5 -> 1.5, equalTo(true)) &&
              assert((hc5.n1, hc5.n2) === 1.5 -> 1.0, equalTo(true))
    ),
    testM("compute the underpoint") {
      for {
        r <- UIO(Ray(Pt(0, 0, -5), Vec.uz))
        w <- WorldTest.oneTransparentCanonicalSphereWorld
        s <- UIO(w.objects.head)
        i <- UIO(Intersection(5, s))
        hc <- WorldHitCompsModule.>.hitComps(r, i, List(i))
      } yield assert(hc.underPoint.z > HitComps.epsilon / 2, equalTo(true)) &&
          assert(hc.hitPt.z < hc.underPoint.z, equalTo(true))
    }
  ).provideManagedShared(Managed.succeed(WorldHitCompsModuleSpecUtil.liveEnv))
)

object WorldHitCompsModuleSpecUtil {
  val liveEnv = new WorldHitCompsModule.Live with NormalReflectModule.Live with LiveFullATModule

}
