package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.drawing.Pattern
import zio.UIO

case class Material(
    pattern: Pattern,
    ambient: Double, //TODO refine Double > 0 && < 1
    diffuse: Double, //TODO refine Double > 0 && < 1
    specular: Double, //TODO refine Double > 0 && < 1
    shininess: Double //TODO refine Double > 10 && < 200
)

object Material {
  def default: UIO[Material] = Pattern.uniform(Color.white).provideM(AffineTransformation.id).map(Material(_, 0.1, 0.9, 0.9, 200d))
}

sealed trait SpatialEntity
object SpatialEntity {
  sealed trait SceneObject {
    def transformation: AffineTransformation
    def material: Material
  }
  object SceneObject {

    //TODO refine Double > 0
    case class PointLight(position: Pt, intensity: Color)

    /**
      * A unit sphere centered in (0, 0, 0) and a transformation on the sphere that puts it  into final position
      * This can be e.g. a chain of transate and shear
      */
    case class Sphere(transformation: AffineTransformation, material: Material) extends SceneObject
    object Sphere {
      def withTransformAndMaterial(tf: AffineTransformation, material: Material): UIO[Sphere] = UIO(tf).zipWith(UIO(material))(Sphere(_, _))
      def unit: UIO[Sphere] =
        for {
          tf  <- AffineTransformation.id
          mat <- Material.default
          res <- withTransformAndMaterial(tf, mat)
        } yield res
    }

    /**
      * Canonical plane {y = 0}
      */
    case class Plane(transformation: AffineTransformation, material: Material) extends SceneObject
    object Plane {
      val horizEpsilon: Double = 1e-4

      def withTransformAndMaterial(tf: AffineTransformation, material: Material): UIO[Plane] = UIO(tf).zipWith(UIO(material))(Plane(_, _))

      def canonical: UIO[Plane] =
        for {
          tf  <- AffineTransformation.id
          mat <- Material.default
          res <- withTransformAndMaterial(tf, mat)
        } yield res
    }
  }

}
