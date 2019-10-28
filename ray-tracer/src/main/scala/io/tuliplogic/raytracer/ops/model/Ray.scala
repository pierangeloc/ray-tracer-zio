package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.affine.PointVec._
import io.tuliplogic.raytracer.geometry.affine.{AT, ATModule}
import io.tuliplogic.raytracer.ops.model.SceneObject.{Plane, Sphere}
import zio.{UIO, URIO, ZIO}

case class Ray(origin: Pt, direction: Vec) {
  def positionAt(t: Double): Pt = origin + (direction * t)
}

case class Intersection(t: Double, sceneObject: SceneObject) //pairs the t where a ray intersects an object, and the object itself

trait RayModule {
  val rayModule: RayModule.Service[Any]
}

//TODO: make a separate operation for canonical intersect in a different module,and build the live version of this in terms of it
object RayModule {

  trait Service[R] {

    /**
      * Calculates ALL the intersections (positive and negative t) between a ray and a canonical scene object
      */
    def canonicalIntersect(ray: Ray, s: SceneObject): URIO[R, List[Intersection]]

    /**
      * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
      */
    def intersect(ray: Ray, o: SceneObject): URIO[R, List[Intersection]]

    /**
      * Calculate the hit of intersections, i.e. the intersection that has the minimal t > 0
      */
    def hit(intersections: List[Intersection]): URIO[R, Option[Intersection]]

    /**
      * Applying an affine transformation to a Ray is applying it to the origin and to the direction
      */
    def transform(at: AT, ray: Ray): URIO[R, Ray]
  }

  trait Live {

    val aTModule: ATModule.Service[Any]
    val rayModule: RayModule.Service[Any] = new Service[Any] {

      def canonicalIntersect(ray: Ray, o: SceneObject): URIO[Any, List[Intersection]] = o match {
        case s@Sphere(_, _) =>
          val sphereToRay = ray.origin - Pt(0, 0, 0)
          val a = ray.direction dot ray.direction
          val b = 2 * (ray.direction dot sphereToRay)
          val c = (sphereToRay dot sphereToRay) - 1

          val delta = b * b - 4 * a * c
          if (delta < 0) UIO.succeed(List())
          else UIO.succeed(List((-b - math.sqrt(delta)) / (2 * a), (-b + math.sqrt(delta)) / (2 * a)).map(Intersection(_, s)))

        case p@Plane(_, _) =>
          if (math.abs(ray.direction.y) < Plane.horizEpsilon)
            UIO(List())
          else {
            val t = -ray.origin.y / ray.direction.y
            UIO(List(Intersection(t, p)))
          }
      }

      /**
        * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
        */
      override def intersect(ray: Ray, o: SceneObject): ZIO[Any, Nothing, List[Intersection]] =
        for {
          inverseTf <- aTModule.invert(o.transformation).orDie
          tfRay <- transform(inverseTf, ray)
          intersections <- canonicalIntersect(tfRay, o)
        } yield intersections

      override def hit(intersections: List[Intersection]): URIO[Any, Option[Intersection]] = UIO {
        intersections.filter(_.t > 0).sortBy(_.t).headOption
      }

      override def transform(at: AT, ray: Ray): URIO[Any, Ray] =
        (for {
          tfPt <- aTModule.applyTf(at, ray.origin)
            tfVec <- aTModule.applyTf(at, ray.direction)
        } yield Ray(tfPt, tfVec)).orDie
    }
  }

  object > extends RayModule.Service[RayModule] {

    /**
      * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
      */
    override def intersect(ray: Ray, o: SceneObject): ZIO[RayModule, Nothing, List[Intersection]] =
      ZIO.accessM(_.rayModule.intersect(ray, o))

    override def hit(intersections: List[Intersection]): URIO[RayModule, Option[Intersection]] =
      ZIO.accessM(_.rayModule.hit(intersections))

    override def transform(at: AT, ray: Ray): URIO[RayModule, Ray] =
      ZIO.accessM(_.rayModule.transform(at, ray))

    override def canonicalIntersect(ray: Ray, o: SceneObject): URIO[RayModule, List[Intersection]] =
      ZIO.accessM(_.rayModule.canonicalIntersect(ray, o))
  }

}
