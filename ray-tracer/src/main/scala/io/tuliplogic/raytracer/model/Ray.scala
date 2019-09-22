package io.tuliplogic.raytracer.model

import io.tuliplogic.geometry.matrix.SpatialEntity.{Pt, Vec}
import io.tuliplogic.geometry.matrix.{MatrixOps, SpatialEntity}
import zio.ZIO

case class Ray(origin: Pt, direction: Vec)

trait RayOps {
  def rayOpsService: RayOps.Service[Any]
}

object RayOps {
  trait Service[R] {
    def positionAt(ray: Ray, t: Double): ZIO[R, Nothing, Pt]
  }

  trait MatrixOpsRayService extends RayOps with MatrixOps {
    def rayOpsService: RayOps.Service[Any] = new Service[Any] {
      override def positionAt(ray: Ray, t: Double): ZIO[Any, Nothing, Pt] =
        for {
          dirCol  <- SpatialEntity.toCol(ray.direction)
          s1      <- matrixOps.times(t, dirCol)
          origCol <- SpatialEntity.toCol(ray.origin)
          resCol  <- matrixOps.add(s1, origCol).orDie
          res     <- SpatialEntity.colToPt(resCol).orDie
        } yield res
    }
  }
}

object rayOperations extends RayOps.Service[RayOps] {
  override def positionAt(ray: Ray, t: Double): ZIO[RayOps, Nothing, Pt] =
    ZIO.accessM(_.rayOpsService.positionAt(ray, t))
}
