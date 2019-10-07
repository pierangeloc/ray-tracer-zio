package io.tuliplogic.raytracer.ops.model

import cats.data.NonEmptyList
import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec._
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject._
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject
import io.tuliplogic.raytracer.geometry.vectorspace.{AffineTransformation, AffineTransformationOps}
import zio.{UIO, URIO, ZIO}

case class Ray(origin: Pt, direction: Vec)
case class Intersection(t: Double, sceneObject: SceneObject) //pairs the t where a ray intersects an object, and the object itself

trait RayOperations {
  def rayOpsService: RayOperations.Service[Any]
}

//TODO: make a separate operation for canonical intersect in a different module,and build the live version of this in terms of it
object RayOperations {
  trait Service[R] {

    def positionAt(ray: Ray, t: Double): URIO[R, Pt]

    /**
      * Calculates ALL the intersections (positive and negative t) between a ray and a canonical scene object
      */
    def canonicalIntersect(ray: Ray, s: SceneObject): URIO[R, List[Intersection]]

    /**
      * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
      */
    def intersect(ray: Ray, o: SceneObject): URIO[R, List[Intersection]]

    def hit(intersections: List[Intersection]): URIO[R, Option[Intersection]]

    def transform(at: AffineTransformation, ray:Ray): URIO[R, Ray]
  }

  trait Live extends RayOperations with MatrixOps with AffineTransformationOps { self =>
    def rayOpsService: RayOperations.Service[Any] = new Service[Any] {
      override def positionAt(ray: Ray, t: Double): ZIO[Any, Nothing, Pt] =
        for {
          dirCol  <- toCol(ray.direction)
          s1      <- matrixOps.times(t, dirCol)
          origCol <- toCol(ray.origin)
          resCol  <- matrixOps.add(s1, origCol).orDie
          res     <- colToPt(resCol).orDie
        } yield res

      def canonicalIntersect(ray: Ray, o: SceneObject): URIO[Any, List[Intersection]] = o match {
        case s@Sphere(_, _) =>
          val sphereToRay = ray.origin - Pt(0, 0, 0)
          val a           = ray.direction dot ray.direction
          val b           = 2 * (ray.direction dot sphereToRay)
          val c           = (sphereToRay dot sphereToRay) - 1

          val delta = b * b - 4 * a * c
          if (delta < 0) UIO.succeed(List())
          else UIO.succeed(List((-b - math.sqrt(delta)) / (2 * a), (-b + math.sqrt(delta)) / (2 * a)).map(Intersection(_, s)))

        case p@Plane(_, _) =>
          if (math.abs(ray.direction.y)  < Plane.horizEpsilon)
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
          inverseTf <- affineTfOps.invert(o.transformation).orDie
          tfRay     <- transform(inverseTf, ray)
          intersections <- canonicalIntersect(tfRay, o)
        } yield intersections

      override def hit(intersections: List[Intersection]): URIO[Any, Option[Intersection]] = UIO {
        intersections.filter(_.t > 0).sortBy(_.t).headOption
      }

      override def transform(at: AffineTransformation, ray: Ray): URIO[Any, Ray] = (for {
        tfPt <- affineTfOps.transform(at, ray.origin)
        tfVec <- affineTfOps.transform(at, ray.direction)
      } yield Ray(tfPt, tfVec)).provide(self).orDie
    }
  }

  object Live extends Live with MatrixOps.Live with AffineTransformationOps.Live
}

object rayOps extends RayOperations.Service[RayOperations] {
  override def positionAt(ray: Ray, t: Double): ZIO[RayOperations, Nothing, Pt] =
    ZIO.accessM(_.rayOpsService.positionAt(ray, t))

  /**
    * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
    */
  override def intersect(ray: Ray, o: SceneObject): ZIO[RayOperations, Nothing, List[Intersection]] =
    ZIO.accessM(_.rayOpsService.intersect(ray, o))

  override def hit(intersections: List[Intersection]): URIO[RayOperations, Option[Intersection]] =
    ZIO.accessM(_.rayOpsService.hit(intersections))

  override def transform(at: AffineTransformation, ray: Ray): URIO[RayOperations, Ray] =
    ZIO.accessM(_.rayOpsService.transform(at, ray))

  override def canonicalIntersect(ray: Ray, o: SceneObject): URIO[RayOperations, List[Intersection]] =
    ZIO.accessM(_.rayOpsService.canonicalIntersect(ray, o))
}
