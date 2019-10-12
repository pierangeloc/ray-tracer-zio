package io.tuliplogic.raytracer.ops.drawing

import cats.data.NonEmptyList
import io.tuliplogic.raytracer.ops.model.{phongOps, rayOps, spatialEntityOps, Color, Intersection, PhongReflection, Ray, RayOperations, SpatialEntityOperations}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, BusinessError, RayTracerError}
import io.tuliplogic.raytracer.ops.model.PhongReflection.HitComps
import zio.{IO, UIO, URIO, ZIO}
import cats.implicits._
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject
import zio.interop.catz._

case class World(pointLight: PointLight, objects: List[SceneObject]) {
  def intersect(ray: Ray): URIO[RayOperations, List[Intersection]] =
    objects.traverse(rayOps.intersect(ray, _)).map(_.flatten.sortBy(_.t))

  def colorAt(ray: Ray): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] =
    for {
      intersections <- intersect(ray)
      maybeHitComps <- intersections.find(_.t > 0).traverse(World.hitComps(ray, _))
      color <- maybeHitComps
        .map(hc =>
          isShadowed(hc.overPoint).flatMap { shadowed =>
            phongOps.lighting(pointLight, hc, shadowed).map(_.toColor)
        })
        .getOrElse(UIO(Color.black))
    } yield color

  def isShadowed(pt: Pt): ZIO[RayOperations, AlgebraicError, Boolean] =
    for {
      v        <- UIO(pointLight.position - pt)
      distance <- v.norm
      vNorm    <- v.normalized
      xs       <- intersect(Ray(pt, vNorm))
      hit      <- rayOps.hit(xs)
    } yield hit.exists(i => i.t > 0 && i.t < distance)
}

object World {
  def hitComps(
      ray: Ray,
      hit: Intersection
  ): ZIO[SpatialEntityOperations with RayOperations, BusinessError.GenericError, HitComps] =
    for {
      pt       <- rayOps.positionAt(ray, hit.t)
      normalV  <- spatialEntityOps.normal(pt, hit.sceneObject)
      eyeV     <- UIO(-ray.direction)
      reflectV <- spatialEntityOps.reflect(ray.direction, normalV)
    } yield HitComps(hit.sceneObject, pt, normalV, eyeV, reflectV)

}
