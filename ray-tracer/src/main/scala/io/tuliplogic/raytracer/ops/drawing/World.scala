package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.ops.model.{Color, Intersection, PhongReflection, Ray, RayOperations, SpatialEntityOperations, phongOps, rayOps, spatialEntityOps}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import io.tuliplogic.raytracer.commons.errors.BusinessError
import io.tuliplogic.raytracer.ops.model.PhongReflection.HitComps
import zio.{IO, UIO, URIO, ZIO}
import cats.implicits._
import zio.interop.catz._

case class World(pointLight: PointLight, objects: List[Sphere]) {
  def intersect(ray: Ray): URIO[RayOperations, List[Intersection]] =
    objects.traverse(rayOps.intersect(ray, _)).map(_.flatten.sortBy(_.t))

  def colorAt(ray: Ray): ZIO[PhongReflection with SpatialEntityOperations with RayOperations, BusinessError.GenericError, Color] = for {
    intersections <- intersect(ray)
    maybeHitComps <- intersections.headOption.traverse(World.hitComps(ray, _))
    color         <- maybeHitComps.map(hc => phongOps.lighting(pointLight, hc).map(_.toColor)).getOrElse(UIO(Color.black))
  } yield color
}

object World {
  def hitComps(
    ray: Ray,
    hit: Intersection
  ): ZIO[SpatialEntityOperations with RayOperations, BusinessError.GenericError, HitComps] =
    hit.sceneObject match {
      case s @ Sphere(_, _) =>
        for {
          pt      <- rayOps.positionAt(ray, hit.t)
            normalV <- spatialEntityOps.normal(pt, s)
            eyeV    <- UIO(-ray.direction)
        } yield HitComps(s, pt, normalV, eyeV)
      case _ => IO.fail(BusinessError.GenericError("can't handle anything but spheres"))
    }
}


