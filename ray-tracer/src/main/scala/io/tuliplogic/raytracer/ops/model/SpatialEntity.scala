package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.ops.drawing.Pattern
import zio.{UIO, URIO, ZIO}

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
    ATModule.>.id.map{ tf =>
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

sealed trait SceneObject {
  def transformation: AT
  def material: Material
}
object SceneObject {

  case class PointLight(position: Pt, intensity: Color)

  /**
    * A unit sphere centered in (0, 0, 0) and a transformation on the sphere that puts it  into final position
    * This can be e.g. a chain of translate and shear
    */
  case class Sphere(transformation: AT, material: Material) extends SceneObject
  object Sphere {
    def withTransformAndMaterial(tf: AT, material: Material): UIO[Sphere] = UIO(tf).zipWith(UIO(material))(Sphere(_, _))
    val canonical: URIO[ATModule, Sphere] =
      for {
        tf  <- ATModule.>.id
        mat <- Material.default
        res <- withTransformAndMaterial(tf, mat)
      } yield res

    val unitGlass: URIO[ATModule, Sphere] =
      for {
        tf     <- ATModule.>.id
        defMat <- Material.default
        mat    <- UIO(defMat.copy(transparency = 1.0, refractionIndex = 1.5))
        res    <- withTransformAndMaterial(tf, mat)
      } yield res

    def make(center: Pt, radius: Double, mat: Material): ZIO[ATModule, AlgebraicError, Sphere] = for {
      scale     <- ATModule.>.scale(radius, radius, radius)
      translate <- ATModule.>.translate(center.x, center.y, center.z)
      composed  <- ATModule.>.compose(scale, translate)
    } yield Sphere(composed, mat)

    def makeUniform(center: Pt, radius: Double, c: Color): ZIO[ATModule, AlgebraicError, Sphere] = for {
      idTf       <- ATModule.>.id
      defaultMat <- Material.default
      newMat     <- UIO(defaultMat.copy(pattern = Pattern.Uniform(c, idTf)))
      s          <- make(center, radius, newMat)
    } yield s
  }

  case class Plane(transformation: AT, material: Material) extends SceneObject
  object Plane {
    val horizEpsilon: Double = 1e-4

    def withTransformAndMaterial(tf: AT, material: Material): UIO[Plane] =
      UIO(Plane(tf, material))

    def make(rotateX: Double, rotateY: Double, rotateZ: Double, passingBy: Pt, material: Material): ZIO[ATModule, AlgebraicError, Plane] = for {
      rotX <- ATModule.>.rotateX(rotateX)
      rotY <- ATModule.>.rotateY(rotateY)
      rotZ <- ATModule.>.rotateZ(rotateZ)
      trn  <- ATModule.>.translate(passingBy.x, passingBy.y, passingBy.z)
      composed <- ATModule.>.compose(rotX, rotY)
        .flatMap(ATModule.>.compose(_, rotZ))
        .flatMap(ATModule.>.compose(_, trn))
    } yield Plane(composed, material)

    /**
      * Canonical plane {y = 0}
      */
    val canonical: URIO[ATModule, Plane] =
      for {
        tf  <- ATModule.>.id
        mat <- Material.default
        res <- withTransformAndMaterial(tf, mat)
      } yield res
  }

}
