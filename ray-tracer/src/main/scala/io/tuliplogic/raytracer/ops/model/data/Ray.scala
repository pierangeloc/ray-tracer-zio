package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.geometry.affine.PointVec._
import Scene.{Plane, Shape, Sphere}
import io.tuliplogic.raytracer.geometry.affine.{AT, aTModule}
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.ops.model.data
import zio.{Has, UIO, URIO, ZIO, ZLayer}

case class Ray(origin: Pt, direction: Vec) {
  def positionAt(t: Double): Pt = origin + (direction * t)
}

case class Intersection(t: Double, sceneObject: Shape) //pairs the t where a ray intersects an object, and the object itself


object rayModule {
  //TODO: make a separate operation for canonical intersect in a different module,and build the live version of this in terms of it
  trait Service {

    /**
      * Calculates ALL the intersections (positive and negative t) between a ray and a canonical scene object
      */
    def canonicalIntersect(ray: Ray, s: Shape): UIO[List[Intersection]]

    /**
      * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
      */
    def intersect(ray: Ray, o: Shape): UIO[List[Intersection]]

    /**
      * Calculate the hit of intersections, i.e. the intersection that has the minimal t > 0
      */
    def hit(intersections: List[Intersection]): UIO[Option[Intersection]]

    /**
      * Applying an affine transformation to a Ray is applying it to the origin and to the direction
      */
    def transform(at: AT, ray: Ray): UIO[Ray]
  }

  type RayModule = Has[Service]

  val live: ZLayer[ATModule, Nothing, RayModule] = ZLayer.fromService[aTModule.Service, RayModule] { aTModule =>
    Has(new Service {

      import Ordering.Double.TotalOrdering

      def canonicalIntersect(ray: Ray, o: Shape): URIO[Any, List[Intersection]] = o match {
        case s@Sphere(_, _) =>
          val sphereToRay = ray.origin - Pt(0, 0, 0)
          val a = ray.direction dot ray.direction
          val b = 2 * (ray.direction dot sphereToRay)
          val c = (sphereToRay dot sphereToRay) - 1

          val delta = b * b - 4 * a * c
          if (delta < 0) UIO.succeed(List())
          else UIO.succeed(List((-b - math.sqrt(delta)) / (2 * a), (-b + math.sqrt(delta)) / (2 * a)).map(data.Intersection(_, s)))

        case p@Plane(_, _) =>
          if (math.abs(ray.direction.y) < Plane.horizEpsilon)
            UIO(List())
          else {
            val t = -ray.origin.y / ray.direction.y
            UIO(List(data.Intersection(t, p)))
          }
      }

      /**
        * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
        */
      override def intersect(ray: Ray, o: Shape): ZIO[Any, Nothing, List[Intersection]] =
        for {
          inverseTf <- aTModule.invert(o.transformation)
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
        } yield Ray(tfPt, tfVec))
    })

  }



  def canonicalIntersect(ray: Ray, s: Shape): URIO[RayModule, List[Intersection]] =
    ZIO.accessM(_.get.canonicalIntersect(ray, s))
  def intersect(ray: Ray, o: Shape): URIO[RayModule, List[Intersection]] =
    ZIO.accessM(_.get.intersect(ray, o))
  def hit(intersections: List[Intersection]): URIO[RayModule, Option[Intersection]] =
    ZIO.accessM(_.get.hit(intersections))
  def transform(at: AT, ray: Ray): URIO[RayModule, Ray] =
    ZIO.accessM(_.get.transform(at, ray))
}

