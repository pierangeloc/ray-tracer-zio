package io.tuliplogic.raytracer.model

import cats.data.NonEmptyList
import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps
import io.tuliplogic.geometry.matrix.SpatialEntity.SceneObject._
import io.tuliplogic.geometry.matrix.SpatialEntity.{Pt, SceneObject, Vec}
import io.tuliplogic.geometry.matrix.{MatrixOps, SpatialEntity}
import zio.{UIO, URIO, ZIO}

case class Ray(origin: Pt, direction: Vec)
case class Intersection(t: Double, sceneObject: SceneObject) //pairs the t where a ray intersects an object, and the object itself

trait RayOps {
  def rayOpsService: RayOps.Service[Any]
}

object RayOps {
  trait Service[R] {
    def positionAt(ray: Ray, t: Double): URIO[R, Pt]

    /**
     * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
     */
    def intersect(ray: Ray, s: Sphere): URIO[R, List[Intersection]]

    def hit(intersections: NonEmptyList[Intersection]): URIO[R, Intersection]
  }

  trait LiveRayService extends RayOps with MatrixOps {
    def rayOpsService: RayOps.Service[Any] = new Service[Any] {
      override def positionAt(ray: Ray, t: Double): ZIO[Any, Nothing, Pt] =
        for {
          dirCol  <- SpatialEntity.toCol(ray.direction)
          s1      <- matrixOps.times(t, dirCol)
          origCol <- SpatialEntity.toCol(ray.origin)
          resCol  <- matrixOps.add(s1, origCol).orDie
          res     <- SpatialEntity.colToPt(resCol).orDie
        } yield res

      /**
       * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
       */
      override def intersect(ray: Ray, s: Sphere): ZIO[Any, Nothing, List[Intersection]] = UIO.succeed {
        val sphereToRay = ray.origin - s.center
        val a = ray.direction dot ray.direction
        val b = 2 * (ray.direction dot sphereToRay)
        val c = (sphereToRay dot sphereToRay) - 1

        val delta = b * b - 4 * a * c
        if (delta < 0) List()
        else List((-b - math.sqrt(delta)) / (2 * a), (-b + math.sqrt(delta)) / (2 * a)).map(Intersection(_, s))
      }

      override def hit(intersections: NonEmptyList[Intersection]): URIO[Any, Intersection] = UIO {
        intersections.foldLeft(intersections.head){
          (oldInt, newInt) => if (oldInt.t <= newInt.t) oldInt else newInt
        }
      }
    }
  }
  object LiveRayService extends LiveRayService with LiveMatrixOps
}

object rayOperations extends RayOps.Service[RayOps] {
  override def positionAt(ray: Ray, t: Double): ZIO[RayOps, Nothing, Pt] =
    ZIO.accessM(_.rayOpsService.positionAt(ray, t))

  /**
   * computes all the t such that ray intersects the sphere. If the ray is tangent to the sphere, 2 equal values are returned
   */
  override def intersect(ray: Ray, s: Sphere): ZIO[RayOps, Nothing, List[Intersection]] =
    ZIO.accessM(_.rayOpsService.intersect(ray, s))

  override def hit(intersections: NonEmptyList[Intersection]): URIO[RayOps, Intersection] =
    ZIO.accessM(_.rayOpsService.hit(intersections))
}
