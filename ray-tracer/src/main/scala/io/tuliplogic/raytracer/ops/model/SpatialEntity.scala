package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformation
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import zio.UIO

sealed trait SpatialEntity

object SpatialEntity {
  sealed trait SceneObject
  object SceneObject {

//    case class Material(color: Color, )
    //TODO refine Double > 0
    case class PointLight(position: Pt, intensity: Double)

    /**
    * A unit sphere centered in (0, 0, 0) and a transformation on the sphere that puts it  into final position
     * This can be e.g. a chain of transate and shear
     */
    case class Sphere(transformation: AffineTransformation) extends SceneObject
    object Sphere {
      def withTransform(tf: AffineTransformation): UIO[Sphere] = UIO(tf).map(Sphere(_))
      def unit: UIO[Sphere] = AffineTransformation.id.flatMap(withTransform)
    }
  }



}
