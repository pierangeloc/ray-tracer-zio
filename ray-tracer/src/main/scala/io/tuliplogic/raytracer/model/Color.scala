package io.tuliplogic.raytracer.model

case class Pixel(x: Int, y: Int)
case class Color(red: Double, green: Double, blue: Double) //TODO: use refined types to enforce something on these colors

object Color {
  val black = Color(0, 0, 0)
  def add(c1: Color, c2: Color): Color = Color(c1.red + c2.red, c1.green + c2.green, c1.blue + c2.blue)
  def mul(c1: Color, c2: Color): Color = Color(c1.red * c2.red, c1.green * c2.green, c1.blue * c2.blue)
}

final case class ColoredPoint(pixel: Pixel, color: Color) {
  override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
}

