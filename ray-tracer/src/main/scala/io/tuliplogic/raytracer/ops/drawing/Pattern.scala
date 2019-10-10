package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.Color
import zio.ZIO

sealed trait Pattern extends (Pt => Color) {
  def transformation: AffineTransformation
}

object Pattern {

  //TODO: see if we can separate better canonical patterns from patterns with transformation
  case class Uniform(c: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = c
  }

  case class Striped(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = if ((math.floor(pt.x) % 2).toInt == 0) c1 else c2
  }

  //Goes from c1 at Pt(0, _, _) to c2 at Pt(1, _, _)
  case class GradientX(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = {
      val dColor = c2 - c1
      c1 + (dColor * (pt.x - math.floor(pt.x)))
    }
  }

  case class Ring(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = {
      val r2 = pt.x * pt.x + pt.z * pt.z
      if ((math.floor(math.sqrt(r2)) % 2) == 0d) c1 else c2
    }
  }

  case class Checker(c1: Color, c2: Color, transformation: AffineTransformation) extends Pattern {
    override def apply(pt: Pt): Color = {
      val edgesSum = math.abs(pt.x) + math.abs(pt.y) + math.abs(pt.z)
      if (math.floor(edgesSum) % 2 == 0) c1 else c2
    }
  }

  def uniform(c: Color): ZIO[AffineTransformation, Nothing, Uniform]                 = ZIO.access(Uniform(c, _))
  def striped(c1: Color, c2: Color): ZIO[AffineTransformation, Nothing, Striped]     = ZIO.access(Striped(c1, c2, _))
  def gradientX(c1: Color, c2: Color): ZIO[AffineTransformation, Nothing, GradientX] = ZIO.access(GradientX(c1, c2, _))
  def ring(c1: Color, c2: Color): ZIO[AffineTransformation, Nothing, Ring]           = ZIO.access(Ring(c1, c2, _))
  def checker(c1: Color, c2: Color): ZIO[AffineTransformation, Nothing, Checker]     = ZIO.access(Checker(c1, c2, _))

}
