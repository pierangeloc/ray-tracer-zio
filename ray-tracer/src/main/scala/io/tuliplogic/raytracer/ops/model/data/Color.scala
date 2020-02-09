package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.commons.errors.BusinessError.GenericError
import zio.IO

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

  def fromHex(s: String): IO[GenericError, Color] = s.toList match {
    case List(r1, r2, g1, g2, b1, b2) =>
      IO.effect(Color(
        Integer.parseInt(r1.toString + r2, 16) / 256.0,
        Integer.parseInt(g1.toString + g2, 16) / 256.0,
        Integer.parseInt(b1.toString + b2, 16) / 256.0
      )).mapError(_ => GenericError(s"could not parse hex RGB $s"))

    case _ => IO.fail(GenericError(s"could not parse hex RGB $s"))
  }
  def add(c1: Color, c2: Color): Color = Color(c1.red + c2.red, c1.green + c2.green, c1.blue + c2.blue)
  def mul(c1: Color, c2: Color): Color = Color(c1.red * c2.red, c1.green * c2.green, c1.blue * c2.blue)
}

final case class ColoredPixel(pixel: Pixel, color: Color) {
  override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
}
