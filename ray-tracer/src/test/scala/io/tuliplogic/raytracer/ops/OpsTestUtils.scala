package io.tuliplogic.raytracer.ops

import io.tuliplogic.raytracer.geometry.TestUtils
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.{HitComps, PhongComponents}
import org.scalactic.TripleEquals._
import org.scalactic.{Equality, Tolerance}
import zio.test.Assertion.{approximatelyEquals, equalTo}
import zio.test.{BoolAlgebra, FailureDetails, assert}

trait OpsTestUtils extends TestUtils with Tolerance {

  implicit val colEq: Equality[Color] = new Equality[Color] {
    override def areEqual(a: Color, b: Any): Boolean = b match {
      case Color(r, g, b) =>
        a.red === r +- 0.001 &&
          a.green === g +- 0.001 &&
          a.blue === b +- 0.001
      case _ => false
    }
  }

  implicit val rayEq: Equality[Ray] = new Equality[Ray] {
    override def areEqual(a: Ray, b: Any): Boolean = b match {
      case Ray(origin, direction) => origin === a.origin && direction === a.direction
      case _                      => false
    }
  }

  implicit val phongComponentsEq: Equality[PhongComponents] = new Equality[PhongComponents] {
    override def areEqual(a: PhongComponents, b: Any): Boolean = b match {
      case PhongComponents(ambient, diffuse, reflective) =>
        ambient === a.ambient && diffuse === a.diffuse && reflective === a.reflective
      case _ => false
    }
  }

  implicit val hitComponentsEq: Equality[HitComps] = new Equality[HitComps] {
    override def areEqual(a: HitComps, b: Any): Boolean = b match {
      case HitComps(obj, pt, normalV, eyeV, rayReflectV, n1, n2) =>
        obj == a.shape &&
          pt === a.hitPt &&
          normalV === a.normalV &&
          eyeV === a.eyeV &&
          rayReflectV === a.rayReflectV &&
          n1 === a.n1 &&
          n2 === a.n2
      case _ => false
    }
  }

  /* ZIO TEST UTILS */

  def assertApproxEqual(hc1: HitComps, hc2: HitComps): BoolAlgebra[FailureDetails] =
    assert(hc1.shape, equalTo(hc2.shape)) &&
      assertApproxEqual(hc1.hitPt, hc2.hitPt) &&
      assertApproxEqual(hc1.normalV, hc2.normalV) &&
      assertApproxEqual(hc1.eyeV, hc2.eyeV) &&
      assertApproxEqual(hc1.rayReflectV, hc2.rayReflectV) &&
      assert(hc1.n1, approximatelyEquals(hc2.n1, 0.001)) &&
      assert(hc1.n2, approximatelyEquals(hc2.n2, 0.001))

  def assertApproxEqual(pt1: Pt, pt2: Pt): BoolAlgebra[FailureDetails] =
    assert(pt1.x, approximatelyEquals(pt2.x, 0.01)) &&
      assert(pt1.y, approximatelyEquals(pt2.y, 0.01)) &&
      assert(pt1.z, approximatelyEquals(pt2.z, 0.01))

  def assertApproxEqual(v1: Vec, v2: Vec): BoolAlgebra[FailureDetails] =
    assert(v1.x, approximatelyEquals(v2.x, 0.01)) &&
      assert(v1.y, approximatelyEquals(v2.y, 0.01)) &&
      assert(v1.z, approximatelyEquals(v2.z, 0.01))

  def assertApproxEqual(c1: Color, c2: Color): BoolAlgebra[FailureDetails] =
    assert(c1.red, approximatelyEquals(c2.red, 0.01)) &&
      assert(c1.green, approximatelyEquals(c2.green, 0.01)) &&
      assert(c1.blue, approximatelyEquals(c2.blue, 0.01))
}
object OpsTestUtils extends OpsTestUtils

trait LiveFullATModule extends ATModule.Live with MatrixModule.BreezeMatrixModule
object LiveFullATModule extends LiveFullATModule