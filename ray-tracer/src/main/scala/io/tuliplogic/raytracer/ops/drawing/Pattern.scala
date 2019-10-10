package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.Color
import zio.ZIO

sealed trait Pattern extends (Pt => Color) {
  def transformation: AffineTransformation
}

object Pattern {

  case class Uniform(c: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = c
  }

  case class Striped(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = if ((math.floor(pt.x) % 2).toInt == 0) c1 else c2
  }

  //Goes from c1 at Pt(0, _, _) to c2 at Pt(1, _, _)
  case class GradientX(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color ={
        val dColor = c2  - c1
        c1 + (dColor * (pt.x - math.floor(pt.x)))
    }
  }

  def uniform(c: Color): ZIO[AffineTransformation, Nothing, Uniform]             = ZIO.access(Uniform(c, _))
  def striped(c1: Color, c2: Color): ZIO[AffineTransformation, Nothing, Striped] = ZIO.access(Striped(c1, c2, _))

}
