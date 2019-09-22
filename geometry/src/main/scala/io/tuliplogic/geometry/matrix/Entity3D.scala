package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.Types.Col

sealed trait Entity3D {
  val col: Col
}

//TODO: define effectful Equals for Pt and Vec
object Entity3D {
  case class Pt(col: Col) extends Entity3D
  case class Vec(col: Col) extends Entity3D
}

