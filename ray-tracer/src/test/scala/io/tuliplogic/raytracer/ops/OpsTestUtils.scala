package io.tuliplogic.raytracer.ops

import io.tuliplogic.raytracer.geometry.TestUtils
import io.tuliplogic.raytracer.ops.model.PhongReflection.PhongComponents
import io.tuliplogic.raytracer.ops.model.{Color, Ray}
import org.scalactic.TripleEquals._
import org.scalactic.{Equality, Tolerance}

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
}
