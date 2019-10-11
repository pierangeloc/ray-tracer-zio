package io.tuliplogic.raytracer.ops.model

import zio.UIO

case class Pixel(x: Int, y: Int)
case class Color(red: Double, green: Double, blue: Double) { //TODO: use refined types to enforce something on these colors, and on alpha
  def *(alpha: Double) = Color(red * alpha, green * alpha, blue * alpha)
  def +(other: Color)  = Color(red + other.red, green + other.green, blue + other.blue)
  def -(other: Color)  = Color(red - other.red, green - other.green, blue - other.blue)
  def *(other: Color)  = Color(red * other.red, green * other.green, blue * other.blue)
}

object Color {

//  make color codes go from 0 to 1, then we can scale them according to the max color code for the encoding standard
  val black = Color(0, 0, 0)
  val white = Color(1, 1, 1)
  val red   = Color(1, 0, 0)
  val green = Color(0, 1, 0)
  val blue  = Color(0, 0, 1)

  def fromHex(s: String): UIO[Color] = ??? //TODO: implement
  def add(c1: Color, c2: Color): Color = Color(c1.red + c2.red, c1.green + c2.green, c1.blue + c2.blue)
  def mul(c1: Color, c2: Color): Color = Color(c1.red * c2.red, c1.green * c2.green, c1.blue * c2.blue)
}

final case class ColoredPoint(pixel: Pixel, color: Color) {
  override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
}
