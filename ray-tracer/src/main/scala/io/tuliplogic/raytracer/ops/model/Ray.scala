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

object RayOperations {
  trait Service[R] {

    def positionAt(ray: Ray, t: Double): URIO[R, Pt]

    /**
      * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
      */
    def intersect(ray: Ray, s: Sphere): URIO[R, List[Intersection]]

    def hit(intersections: NonEmptyList[Intersection]): URIO[R, Intersection]

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

      /**
        * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
        */
      override def intersect(ray: Ray, s: Sphere): ZIO[Any, Nothing, List[Intersection]] = {
        def intersectUnitSphere(r: Ray): List[Intersection] = {
          val sphereToRay = ray.origin - Pt(0, 0, 0)
          val a           = ray.direction dot ray.direction
          val b           = 2 * (ray.direction dot sphereToRay)
          val c           = (sphereToRay dot sphereToRay) - 1

          val delta = b * b - 4 * a * c
          if (delta < 0) List()
          else List((-b - math.sqrt(delta)) / (2 * a), (-b + math.sqrt(delta)) / (2 * a)).map(Intersection(_, s))
        }
        for {
          inverseTf <- affineTfOps.invert(s.transformation).orDie
          tfRay     <- transform(inverseTf, ray)
        } yield intersectUnitSphere(tfRay)
      }

      override def hit(intersections: NonEmptyList[Intersection]): URIO[Any, Intersection] = UIO {
        intersections.foldLeft(intersections.head) { (oldInt, newInt) =>
          if (newInt.t >= 0 && newInt.t <= oldInt.t) newInt else oldInt
        }
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
  override def intersect(ray: Ray, s: Sphere): ZIO[RayOperations, Nothing, List[Intersection]] =
    ZIO.accessM(_.rayOpsService.intersect(ray, s))

  override def hit(intersections: NonEmptyList[Intersection]): URIO[RayOperations, Intersection] =
    ZIO.accessM(_.rayOpsService.hit(intersections))

  override def transform(at: AffineTransformation, ray: Ray): URIO[RayOperations, Ray] =
    ZIO.accessM(_.rayOpsService.transform(at, ray))
}
