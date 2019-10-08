package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.Color

sealed trait Pattern extends (Pt => Color)
object Pattern {
  case class Uniform(c: Color) extends Pattern {
    override def apply(pt: Pt): Color = c
  }
  case class Striped(c1: Color, c2: Color) extends Pattern {
    override def apply(pt: Pt): Color = if ((math.floor(pt.x) % 2).toInt == 0) c1 else c2
  }
}
