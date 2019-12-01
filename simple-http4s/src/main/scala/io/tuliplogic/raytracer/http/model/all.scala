package io.tuliplogic.raytracer.http.model

case class Material(
  pattern: Pattern,
  ambient: Option[Double], //TODO refine Double > 0 && < 1
  diffuse: Option[Double], //TODO refine Double > 0 && < 1
  specular: Option[Double], //TODO refine Double > 0 && < 1 specularity of the surface to the light source
  shininess: Option[Double], //TODO refine Double > 10 && < 200 shininess of the surface to the light source
  reflective: Option[Double], //TODO refine Double [0, 1] generic reflectiveness of the surface, of generic rays not only coming from the light sourcex
  transparency: Option[Double], //TODO refine Double [0, 1] how transparent the material is
  refractionIndex: Option[Double] //TODO refine Double [0, 1] the material refraction index (for vacuum it's 1)
)

sealed trait Pattern
object Pattern {
  case class Striped(c1: String, c2: String, stripSize: Double) extends Pattern
  case class Uniform(c: String) extends Pattern
  case class Checker(c1: String, c2: String, size: Double) extends Pattern
}

sealed trait Shape
object Shape {
  case class Plane(
    material: Material,
    rotateX: Option[Double],
    rotateY: Option[Double],
    rotateZ: Option[Double],
    translateX: Option[Double],
    translateY: Option[Double],
    translateZ: Option[Double],
  ) extends Shape

  case class Sphere(
    material: Material,
    centerX: Double,
    centerY: Double,
    centerZ: Double,
    radius: Double
  ) extends Shape
}

case class PointLight(ptX: Double, ptY: Double, ptZ: Double, color: String)
case class Camera(
  fromX: Double,
  fromY: Double,
  fromZ: Double,
  toX: Double,
  toY: Double,
  toZ: Double,
  upX: Double,
  upY: Double,
  upZ: Double,
  hRes: Int,
  vRes: Int
)
case class Scene(shapes: List[Shape], pointLight: PointLight, camera: Camera)