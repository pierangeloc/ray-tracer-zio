package io.tuliplogic.raytracer.ops

import io.tuliplogic.raytracer.ops.model.Color
import org.scalactic.TripleEquals._
import org.scalactic.{Equality, Tolerance}

trait OpsTestUtils extends Tolerance {

  implicit val vecEq: Equality[Color] = new Equality[Color] {
    override def areEqual(a: Color, b: Any): Boolean = b match {
      case Color(r, g, b) =>
        a.red   === r +- 0.001
        a.green === g +- 0.001
        a.blue  === b +- 0.001
      case _ => false
    }
  }
}
