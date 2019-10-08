package io.tuliplogic.raytracer.ops.drawing

import cats.data.NonEmptyList
import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, BusinessError, RayTracerError}
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.drawing.Scene.RichRayOperations
import io.tuliplogic.raytracer.ops.model.PhongReflection.{HitComps, PhongComponents}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{
  phongOps,
  rayOps,
  spatialEntityOps,
  Canvas,
  Intersection,
  Material,
  PhongReflection,
  Ray,
  RayOperations,
  SpatialEntityOperations
}
import zio.interop.catz._
import zio.stream._
import zio.{Chunk, IO, UIO, ZIO}

import scala.{Stream => ScalaStream}

/**
  * This represents a rectangle where our scene gets projected, if you want it's the camera, lying centered on the z axis, defined by its (half) w, h
  * and sampled with a given resolution. We are interested in producing the 3d position of every pixel in order to calculate the ray
  */
case class SampledRect(halfWidth: Double, halfHeight: Double, z: Double, hres: Int, vres: Int) {

  type UStreamC[A] = StreamChunk[Nothing, A]

  def pixels(chunkSize: Int): ScalaStream[Chunk[(Pt, Int, Int)]] =
    (for {
      xn <- ScalaStream.from(0).take(hres)
      yn <- ScalaStream.from(0).take(vres)
    } yield {
      val cX = xn * (halfWidth * 2) / hres
      val cY = yn * (halfHeight * 2) / vres
      (Pt(cX - halfWidth, -cY + halfHeight, z), xn, yn)
    }).grouped(chunkSize)
      .map(str => Chunk.fromIterable(str))
      .toStream

  def pixelsChunkedStream: UStreamC[(Pt, Int, Int)] = StreamChunk(Stream.fromIterable(pixels(4096)))
}

/**
  * All the logic necessary to determine if a ray intersects a sphere, and upon intersection determine its Phong components
  */
case class Scene(infinitePoint: Pt, pointLight: PointLight) {

  private def rayForPixel(px: Pt): IO[AlgebraicError, Ray] =
    for {
      normalizedDirection <- (px - infinitePoint).normalized
    } yield Ray(origin = infinitePoint, direction = normalizedDirection)

  private def hitComps(
      ray: Ray,
      hit: Intersection
  ): ZIO[PhongReflection with SpatialEntityOperations with RayOperations, BusinessError.GenericError, HitComps] =
    hit.sceneObject match {
      case s @ Sphere(_, _) =>
        for {
          pt      <- rayOps.positionAt(ray, hit.t)
          normalV <- spatialEntityOps.normal(pt, s)
          eyeV    <- UIO(-ray.direction)
        } yield HitComps(s, pt, normalV, eyeV)
      case _ => IO.fail(BusinessError.GenericError("can't handle anything but spheres"))
    }

  private def colorForHit(
      ray: Ray,
      hit: Intersection
  ): ZIO[PhongReflection with SpatialEntityOperations with RayOperations, BusinessError.GenericError, PhongReflection.PhongComponents] =
    hitComps(ray, hit).flatMap { hitComps =>
      phongOps.lighting(pointLight, hitComps, false)
    }

  def intersectAndComputePhong(canvasPx: Pt, sphere: Sphere): ZIO[RichRayOperations, RayTracerError, Option[PhongComponents]] =
    for {
      ray           <- rayForPixel(canvasPx)
      intersections <- rayOps.intersect(ray, sphere)
      maybeHit      <- rayOps.hit(intersections)
      maybeComps    <- maybeHit.map(colorForHit(ray, _)).sequence
    } yield maybeComps

}

object Scene {
  type RichRayOperations = PhongReflection with SpatialEntityOperations with RayOperations
  object RichRayOperations {
    trait Live extends PhongReflection.Live with SpatialEntityOperations.Live with RayOperations.Live
  }
}
