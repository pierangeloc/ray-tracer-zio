package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.commons.errors.ATError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.affine.AT
import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
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
        tf  <- aTModule.>.id
        mat <- Material.default
        res <- withTransformAndMaterial(tf, mat)
      } yield res

    val unitGlass: URIO[ATModule, Sphere] =
      for {
        tf     <- aTModule.>.id
        defMat <- Material.default
        mat    <- UIO(defMat.copy(transparency = 1.0, refractionIndex = 1.5))
        res    <- withTransformAndMaterial(tf, mat)
      } yield res

    def make(center: Pt, radius: Double, mat: Material): ZIO[ATModule, ATError, Sphere] = for {
      scale     <- aTModule.>.scale(radius, radius, radius)
      translate <- aTModule.>.translate(center.x, center.y, center.z)
      composed  <- aTModule.>.compose(scale, translate)
    } yield Sphere(composed, mat)

    def makeUniform(center: Pt, radius: Double, c: Color): ZIO[ATModule, ATError, Sphere] = for {
      idTf       <- aTModule.>.id
      defaultMat <- Material.default
      newMat     <- UIO(defaultMat.copy(pattern = Pattern.Uniform(c, idTf)))
      s          <- make(center, radius, newMat)
    } yield s
  }

  case class Plane(transformation: AT, material: Material) extends Shape
  object Plane {
    val horizEpsilon: Double = 1e-6

    def withTransformAndMaterial(tf: AT, material: Material): UIO[Plane] =
      UIO(Plane(tf, material))

    def make(rotateX: Double = 0, rotateY: Double = 0, rotateZ: Double = 0, passingBy: Pt = Pt.origin, material: Material): ZIO[ATModule, ATError, Plane] = for {
      rotX <- aTModule.>.rotateX(rotateX)
      rotY <- aTModule.>.rotateY(rotateY)
      rotZ <- aTModule.>.rotateZ(rotateZ)
      trn  <- aTModule.>.translate(passingBy.x, passingBy.y, passingBy.z)
      composed <- aTModule.>.compose(rotX, rotY)
        .flatMap(aTModule.>.compose(_, rotZ))
        .flatMap(aTModule.>.compose(_, trn))
    } yield Plane(composed, material)

    /**
      * Canonical plane {y = 0}
      */
    val canonical: URIO[ATModule, Plane] =
      for {
        tf  <- aTModule.>.id
        mat <- Material.default
        res <- withTransformAndMaterial(tf, mat)
      } yield res
  }

}
