package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.geometry.affine.ATModule
import zio.URIO

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
    ATModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.1, diffuse = 0.9, specular = 0.9, shininess = 200d, reflective = 0, transparency = 0, refractionIndex = 1)
    }

  val glass: URIO[ATModule, Material] =
    ATModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.0, diffuse = 0.0, specular = 0.1, shininess = 200d, reflective = 0.4, transparency = 0.95, refractionIndex = 1.5)
    }
  val air: URIO[ATModule, Material] =
    ATModule.>.id.map { tf =>
      Material(Pattern.Uniform(Color.white, tf), ambient = 0.0, diffuse = 0.0, specular = 0, shininess = 0, reflective = 0, transparency = 1, refractionIndex = 1)
    }
}
