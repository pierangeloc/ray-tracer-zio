package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.random.Random
import zio.test._
import zio.test.Assertion._
import PointVecTestUtil.Generators._
import PointVecTestUtil._

object PointVecTest extends DefaultRunnableSpec(suite("Points and Vectors form an affine space") (
    testM("vectors form a group")(
      check(vecGen, vecGen, vecGen) { (v1, v2, v3) =>
        assertApprox  (v1 + (v2 + v3), (v1 + v2) + v3) &&
        assertApprox (v1 + v2 , v2 + v1) &&
        assertApprox (v1 + Vec.zero , Vec.zero + v1)
      }
    ),
    testM("vectors and points form an affine space") (
      check(ptGen, ptGen) { (p1, p2) =>
        assertApprox (p2, p1 + (p2 - p1))
      }
    )
  )
)

object PointVecTestUtil {
  object Generators {
    val ptGen: Gen[Random, Pt] = for {
      x <- Gen.double(-1000, 1000)
      y <- Gen.double(-1000, 1000)
      z <- Gen.double(-1000, 1000)
    } yield Pt(x, y, z)

    val vecGen: Gen[Random, Vec] = for {
      x <- Gen.double(-1000, 1000)
      y <- Gen.double(-1000, 1000)
      z <- Gen.double(-1000, 1000)
    } yield Vec(x, y, z)
  }

  def assertApprox(pt1: Pt, pt2: Pt): BoolAlgebra[FailureDetails] =
    assert(pt1.x, approximatelyEquals(pt2.x, 0.01)) &&
    assert(pt1.y, approximatelyEquals(pt2.y, 0.01)) &&
    assert(pt1.z, approximatelyEquals(pt2.z, 0.01))

  def ≈(pt1: Pt, pt2: Pt): BoolAlgebra[FailureDetails] = assertApprox(pt1, pt2)

  def assertApprox(v1: Vec, v2: Vec): BoolAlgebra[FailureDetails] =
    assert(v1.x, approximatelyEquals(v2.x, 0.01)) &&
    assert(v1.y, approximatelyEquals(v2.y, 0.01)) &&
    assert(v1.z, approximatelyEquals(v2.z, 0.01))

  def ≈(v1: Vec, v2: Vec): BoolAlgebra[FailureDetails] = assertApprox(v1, v2)

}
