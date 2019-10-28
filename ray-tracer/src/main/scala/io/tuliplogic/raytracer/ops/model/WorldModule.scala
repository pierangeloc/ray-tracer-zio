package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.ops.drawing.World
import zio.{UIO, ZIO}
import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, RayTracerError}
import io.tuliplogic.raytracer.commons.errors.BusinessError.GenericError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.PhongReflectionModule.HitComps
import zio.interop.catz._

trait WorldModule {
  val worldModule: WorldModule.Service[Any]
}

object WorldModule {
  trait Service[R] {

    /**
      * Provides the list of intersections between a Ray and the world
      */
    def intersections(world: World, ray: Ray): ZIO[R, Nothing, List[Intersection]]

    /**
      * Determines if the LOS of a point to the light source is shadowed by an object of the world
      */
    def isShadowed(world: World, pt: Pt): ZIO[R, AlgebraicError, Boolean]

    /**
      * Determines the hit components for an intersection, among the list of intersections
      * This is important to handle transparency, as the first intersection is not sufficient, and we must handle also subsequent ones to determine
      * the refraction indexes between materials
      */
    def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[R, GenericError, HitComps]

    /**
      * Determines the full color for a ray that hits the world.
      * This is the main method that should be called by a renderer
      */
    def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[R, RayTracerError, Color]
  }

  trait Live extends WorldModule {
    val rayModule: RayModule.Service[Any]
    val normalReflectModule: NormalReflectModule.Service[Any]
    val worldReflectionModule: WorldReflectionModule.Service[Any]
    val worldRefractionModule: WorldRefractionModule.Service[Any]
    val phongReflectionModule: PhongReflectionModule.Service[Any]

    override val worldModule: Service[Any] = new Service[Any] {

      def intersections(world: World, ray: Ray): ZIO[Any, Nothing, List[Intersection]] =
        world.objects.traverse(rayModule.intersect(ray, _)).map(_.flatten.sortBy(_.t))

      def isShadowed(world: World, pt: Pt): ZIO[Any, AlgebraicError, Boolean] =
        for {
          v        <- UIO(world.pointLight.position - pt)
          distance <- v.norm
          vNorm    <- v.normalized
          xs       <- intersections(world, Ray(pt, vNorm))
          hit      <- rayModule.hit(xs)
        } yield hit.exists(i => i.t > 0 && i.t < distance)

      override def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[Any, GenericError, HitComps] = {
        type Z = (List[SceneObject], Option[Double], Option[Double])
        /**
          * We can calculate the n1, n2 for the hit, given the list of intersections. Each intersection carries the object, together with its material
          * @return
          */
        def n1n2: UIO[(Double, Double)] = {

          val maybeN1N2: (List[SceneObject], Option[Double], Option[Double]) = intersections.foldLeft[Z]((Nil, None, None)) {
            case (in@(xs, Some(n1), Some(n2)), _) =>
              in
            case ((conts, None, None), `hit`) =>
              val n1: Double = conts.lastOption.map(_.material.refractionIndex).getOrElse(1)
              val contss =
                if (conts.contains(hit.sceneObject))
                  conts.filter(_ != hit.sceneObject)
                else
                  conts :+ hit.sceneObject
              val n2: Double = contss.lastOption.map(_.material.refractionIndex).getOrElse(1)
              (contss, Some(n1), Some(n2))

            case ((conts, None, None), i) =>
              val contss = if (conts.contains(i.sceneObject)) conts.filter(_ != i.sceneObject)
              else conts :+ i.sceneObject
              (contss, None, None)

            case _ => (Nil, None, None)
          }

          maybeN1N2 match {
            case (_, Some(n1), Some(n2)) => UIO.succeed((n1, n2))
            case _ => ZIO.die(new IllegalArgumentException("can't determine refraction indexes")) //TODO: proper error mgmgt
          }

        }

        for {
          pt       <- UIO(ray.positionAt(hit.t))
          normalV  <- normalReflectModule.normal(pt, hit.sceneObject)
          eyeV     <- UIO(-ray.direction)
          reflectV <- normalReflectModule.reflect(ray.direction, normalV)
          (n1, n2) <- n1n2
        } yield HitComps(hit.sceneObject, pt, normalV, eyeV, reflectV, n1, n2)
      }

      def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[Any, RayTracerError, Color] =
        for {
          intersections <- intersections(world, ray)
          maybeHitComps <- intersections.find(_.t > 0).traverse(hitComps(ray, _, intersections))
            color <- maybeHitComps
              .map(hc =>
                for {
                  shadowed <- isShadowed(world, hc.overPoint)
                    color <- phongReflectionModule.lighting(world.pointLight, hc, shadowed).map(_.toColor)
                    //invoke this only if remaining > 0. Also, reflected color and color can be computed in parallel
                    reflectedColor <- if (remaining > 0) worldReflectionModule.reflectedColor(world, hc, remaining - 1) else UIO(Color.black)
                    refractedColor <- worldRefractionModule.refractedColor(world, hc, remaining)
                } yield color + reflectedColor + refractedColor
              ).getOrElse(UIO(Color.black))
        } yield color

    }
  }

  object > extends WorldModule.Service[WorldModule] {
    override def intersections(world: World, ray: Ray): ZIO[WorldModule, Nothing, List[Intersection]] =
      ZIO.accessM(_.worldModule.intersections(world, ray))
    override def isShadowed(world: World, pt: Pt): ZIO[WorldModule, AlgebraicError, Boolean] =
      ZIO.accessM(_.worldModule.isShadowed(world, pt))
    override def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[WorldModule, GenericError, HitComps] =
      ZIO.accessM(_.worldModule.hitComps(ray, hit, intersections))
    override def colorForRay(world: World, ray: Ray, remaining: Int): ZIO[WorldModule, RayTracerError, Color] =
      ZIO.accessM(_.worldModule.colorForRay(world, ray, remaining))
  }
}