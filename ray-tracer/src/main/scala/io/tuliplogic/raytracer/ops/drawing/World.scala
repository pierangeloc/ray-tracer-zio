package io.tuliplogic.raytracer.ops.drawing

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

  def colorAt(ray: Ray, remaining: Int = 5): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] =
    for {
      intersections <- intersect(ray)
      maybeHitComps <- intersections.find(_.t > 0).traverse(World.hitComps(ray, _))
      color <- maybeHitComps
        .map(hc =>
          for {
            shadowed <- isShadowed(hc.overPoint)
            color <- phongOps.lighting(pointLight, hc, shadowed).map(_.toColor)
            //invoke this only if remaining > 0. Also, reflected color and color can be computed in parallel
            reflectedColor <- if (remaining > 0) World.reflectedColor(this, hc, remaining - 1) else UIO(Color.black)
          } yield color + reflectedColor
        ).getOrElse(UIO(Color.black))
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
  //TODO: move all these things into a service, no methods on the world. Make World just data, plus some syntax for utility

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


  //TODO: make remaining part of an environment initialized with Ref
  def reflectedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] =
    if (hitComps.obj.material.reflective == 0) {
      UIO(Color.black)
    } else {
      val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
      world.colorAt(reflRay, remaining).map(c =>
        c * hitComps.obj.material.reflective
      )
    }

}
