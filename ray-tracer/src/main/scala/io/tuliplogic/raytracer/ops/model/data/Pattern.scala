package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.geometry.affine.AT
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt

/**
  * A Pattern is a way to give a color to a surface point
  * Every pattern has an associated transformation, the same way a shape has an associated transformation
  */
sealed trait Pattern extends (Pt => Color) {
  def transformation: AT
}

object Pattern {
  val epsilon: Double = 1e-6

  //TODO: see if we can separate better canonical patterns from patterns with transformation
  case class Uniform(c: Color, transformation: AT) extends Pattern {
    override def apply(pt: Pt): Color = c
  }

  case class Striped(c1: Color, c2: Color, transformation: AT) extends Pattern {
    override def apply(pt: Pt): Color = if ((math.floor(pt.x) % 2).toInt == 0) c1 else c2
  }

  //Goes from c1 at Pt(0, _, _) to c2 at Pt(1, _, _)
  case class GradientX(c1: Color, c2: Color, transformation: AT) extends Pattern {
    override def apply(pt: Pt): Color = {
      val dColor = c2 - c1
      c1 + (dColor * (pt.x - math.floor(pt.x)))
    }
  }

  case class Ring(c1: Color, c2: Color, transformation: AT) extends Pattern {
    override def apply(pt: Pt): Color = {
      val r2 = pt.x * pt.x + pt.z * pt.z
      if ((math.floor(math.sqrt(r2)) % 2) == 0d) c1 else c2
    }
  }

  case class Checker(c1: Color, c2: Color, transformation: AT) extends Pattern {
    override def apply(pt: Pt): Color = {
      val edgesSum = math.floor(pt.x + epsilon) + math.floor(pt.y + epsilon) + math.floor(pt.z + epsilon)
      if (math.floor(edgesSum) % 2 == 0) c1 else c2
    }
  }
}
