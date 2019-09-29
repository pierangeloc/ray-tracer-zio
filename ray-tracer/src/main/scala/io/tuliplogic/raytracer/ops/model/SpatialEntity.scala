package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import zio.UIO

case class Material(
  color: Color,
  ambient: Double,  //TODO refine Double > 0 && < 1
  diffuse: Double,  //TODO refine Double > 0 && < 1
  specular: Double, //TODO refine Double > 0 && < 1
  shininess: Double //TODO refine Double > 10 && < 200
)

object Material {
  def default: Material = Material(Color.white, 0.1, 0.9, 0.9, 200d)
}

sealed trait SpatialEntity
object SpatialEntity {
  sealed trait SceneObject
  object SceneObject {

//    case class Material(color: Color, )
    //TODO refine Double > 0
    case class PointLight(position: Pt, intensity: Color)

    /**
    * A unit sphere centered in (0, 0, 0) and a transformation on the sphere that puts it  into final position
     * This can be e.g. a chain of transate and shear
     */
    case class Sphere(transformation: AffineTransformation, material: Material) extends SceneObject
    object Sphere {
      def withTransformAndMaterial(tf: AffineTransformation, material: Material): UIO[Sphere] = UIO(tf).zipWith(UIO(material))(Sphere(_, _))
      def unit: UIO[Sphere] = for {
        tf <- AffineTransformation.id
        res <- withTransformAndMaterial(tf, Material.default)
      } yield res
    }
  }



}
