package io.tuliplogic.raytracer.geometry

import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import org.scalactic.TripleEquals._
import org.scalactic.{Equality, Tolerance}

trait TestUtils extends Tolerance {

  implicit val vecEq: Equality[Vec] = new Equality[Vec] {
    override def areEqual(a: Vec, b: Any): Boolean = b match {
      case Vec(x, y, z) =>
        a.x === x +- 0.01 &&
          a.y === y +- 0.01 &&
          a.z === z +- 0.01
      case _ => false
    }
  }

  implicit val ptEq: Equality[Pt] = new Equality[Pt] {
    override def areEqual(a: Pt, b: Any): Boolean = b match {
      case Pt(x, y, z) =>
        a.x === x +- 0.01 &&
          a.y === y +- 0.01 &&
          a.z === z +- 0.01
      case _ => false
    }
  }
}
