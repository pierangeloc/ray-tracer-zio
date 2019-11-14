package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.PointVecTestUtil.Generators
import zio.random.Random
import zio.test._
import zio.test.Assertion._

object PointVecTest extends DefaultRunnableSpec(suite("Points and Vectors form an affine space") {
  testM("bijection of translations") {
    check(Generators.ptGen, Generators.ptGen) { (p1, p2) =>
      PointVecTestUtil.assertApproxEqual(p2, p1 + (p2 - p1))
    }
  }
})

object PointVecTestUtil {
  object Generators {
    val ptGen: Gen[Random, Pt] = for {
      x <- Gen.double(-1000, 1000)
      y <- Gen.double(-1000, 1000)
      z <- Gen.double(-1000, 1000)
    } yield Pt(x, y, z)
  }

  def assertApproxEqual(pt1: Pt, pt2: Pt): BoolAlgebra[FailureDetails] =
    assert(pt1.x, approximatelyEquals(pt2.x, 0.01)) &&
      assert(pt1.y, approximatelyEquals(pt2.y, 0.01)) &&
      assert(pt1.z, approximatelyEquals(pt2.z, 0.01))

  def assertApproxEqual(v1: Vec, v2: Vec): BoolAlgebra[FailureDetails] =
    assert(v1.x, approximatelyEquals(v2.x, 0.01)) &&
      assert(v1.y, approximatelyEquals(v2.y, 0.01)) &&
      assert(v1.z, approximatelyEquals(v2.z, 0.01))
}
