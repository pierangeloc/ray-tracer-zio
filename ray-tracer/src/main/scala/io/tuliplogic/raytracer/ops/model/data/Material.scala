package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import zio.{URIO, ZIO}

case class Material(
  pattern: Pattern,
  ambient: Double, //TODO refine Double > 0 && < 1
  diffuse: Double, //TODO refine Double > 0 && < 1
  specular: Double, //TODO refine Double > 0 && < 1 specularity of the surface to the light source
  shininess: Double, //TODO refine Double > 10 && < 200 shininess of the surface to the light source
  reflective: Double, //TODO refine Double [0, 1] generic reflectiveness of the surface, of generic rays not only coming from the light sourcex
  transparency: Double, //TODO refine Double [0, 1] how transparent the material is
  refractionIndex: Double //TODO refine Double [0, 1] the material refraction index (for vacuum it's 1)
)

object Material {
  def default: URIO[ATModule, Material] =
    aTModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.1, diffuse = 0.9, specular = 0.9, shininess = 200d, reflective = 0, transparency = 0, refractionIndex = 1)
    }
  def uniform(c: Color, diffuse: Double = 0.7, specular: Double = 0.9, shininess: Double = 50, reflective: Double = 0): URIO[ATModule, Material] = for {
    mat  <- Material.default
    idTf <- aTModule.>.id
  } yield mat.copy(
    pattern = Pattern.Uniform(c, idTf),
    diffuse = diffuse,
    specular = specular,
    shininess = shininess,
    reflective = reflective
  )

  //TODO: fix this
  def gradientSuperPower(from: Color, to: Color, fromX: Double = -1, toX: Double = -1, rotateY: Double = 0, diffuse: Double = 0.7, specular: Double = 0.9, shininess: Double = 50, reflective: Double = 0): URIO[ATModule, Material] = for {
    mat  <- Material.default
    scTf <- aTModule.>.scale(toX - fromX, 1, 1)
    rtTf <- aTModule.>.rotateY(rotateY)
    trTf <- aTModule.>.translate((toX - fromX) / 2, 0, 0)
    composed <- aTModule.>.compose(scTf, rtTf).flatMap(aTModule.>.compose(_, trTf))
  } yield mat.copy(
    pattern = Pattern.GradientX(from, to, composed),
    diffuse = diffuse,
    specular = specular,
    shininess = shininess,
    reflective = reflective
  )

  def gradient(from: Color, to: Color, diffuse: Double = 0.7, specular: Double = 0.9, shininess: Double = 50, reflective: Double = 0): ZIO[ATModule, Nothing, Material] = for {
    mat  <- Material.default
      idTf <- aTModule.>.id
  } yield mat.copy(
    pattern = Pattern.GradientX(from, to, idTf),
    diffuse = diffuse,
    specular = specular,
    shininess = shininess
  )

  def striped(c1: Color, c2: Color, stripSize: Double, diffuse: Double = 0.7, specular: Double = 0.9, shininess: Double = 50, reflective: Double = 0): URIO[ATModule, Material] = for {
    mat  <- Material.default
    tf <- aTModule.>.scale(stripSize, 1, 1)
  } yield mat.copy(
    pattern = Pattern.Striped(c1, c2, tf),
    diffuse = diffuse,
    specular = specular,
    shininess = shininess,
    reflective = reflective
  )

  val glass: URIO[ATModule, Material] =
    aTModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.0, diffuse = 0.0, specular = 0.1, shininess = 200d, reflective = 0.4, transparency = 0.95, refractionIndex = 1.5)
    }

  val opaquegGlass: URIO[ATModule, Material] =
    aTModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.0, diffuse = 0.0, specular = 0.1, shininess = 200d, reflective = 0.1, transparency = 0.95, refractionIndex = 1.5)
    }

  val air: URIO[ATModule, Material] =
    aTModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.0, diffuse = 0.0, specular = 0, shininess = 0, reflective = 0, transparency = 1, refractionIndex = 1)
    }
}
