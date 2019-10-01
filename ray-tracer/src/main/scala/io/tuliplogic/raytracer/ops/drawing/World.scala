package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.ops.model.{Intersection, Ray, RayOperations, rayOps}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import zio.interop.catz._
import cats.implicits._
import zio.URIO

case class World(pointLight: PointLight, objects: List[Sphere]) {
  def intersect(ray: Ray): URIO[RayOperations, List[Intersection]] =
    objects.traverse(rayOps.intersect(ray, _)).map(_.flatten.sortBy(_.t))
}


