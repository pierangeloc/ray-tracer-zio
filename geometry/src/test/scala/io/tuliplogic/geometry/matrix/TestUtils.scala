package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.SpatialEntity.Vec
import org.scalactic.{Equality, Tolerance}
import org.scalactic.TripleEquals._

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
