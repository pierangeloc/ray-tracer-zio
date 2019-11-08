package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import zio.{UIO, URIO, ZIO}

object Scene {

  case class PointLight(position: Pt, intensity: Color)

  sealed trait Shape {
    def transformation: AT
    def material: Material
  }

  /**
    * A unit sphere centered in (0, 0, 0) and a transformation on the sphere that puts it  into final position
    * This can be e.g. a chain of translate and shear
    */
  case class Sphere(transformation: AT, material: Material) extends Shape
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

  case class Plane(transformation: AT, material: Material) extends Shape
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
