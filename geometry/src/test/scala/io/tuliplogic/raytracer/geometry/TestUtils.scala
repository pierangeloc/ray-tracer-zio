package io.tuliplogic.raytracer.geometry

import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Vec
import org.scalactic.TripleEquals._
import org.scalactic.{Equality, Tolerance}

trait TestUtils extends Tolerance {

  implicit val vecEq: Equality[Vec] = new Equality[Vec] {
    override def areEqual(a: Vec, b: Any): Boolean = b match {
      case Vec(x, y, z) =>
        a.x === x +- 0.001
        a.y === y +- 0.001
        a.z === z +- 0.001
      case _ => false
    }
  }
}
