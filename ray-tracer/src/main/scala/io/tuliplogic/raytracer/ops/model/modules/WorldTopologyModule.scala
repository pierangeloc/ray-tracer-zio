package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.data.{Intersection, Ray, RayModule, World}
import zio.{UIO, ZIO}

/**
 * 
 * This module provides capabilities to deal purely with the topological structure of our space, i.e. it doesn't bother about
 * materials and the like
 */
trait WorldTopologyModule {
  val worldTopologyModule: WorldTopologyModule.Service[Any]
}

object WorldTopologyModule {
  trait Service[R] {
    /**
      * Provides the list of intersections between a Ray and the world
      */
    def intersections(world: World, ray: Ray): ZIO[R, Nothing, List[Intersection]]

    /**
      * Determines if the LOS of a point to the light source is shadowed by an object of the world
      */
    def isShadowed(world: World, pt: Pt): ZIO[R, Nothing, Boolean]
  }

  trait Live extends WorldTopologyModule {
    val rayModule: RayModule.Service[Any]

    override val worldTopologyModule: Service[Any] = new Service[Any] {

      def intersections(world: World, ray: Ray): ZIO[Any, Nothing, List[Intersection]] =
        ZIO.traverse(world.objects)(rayModule.intersect(ray, _)).map(_.flatten.sortBy(_.t))

      def isShadowed(world: World, pt: Pt): ZIO[Any, Nothing, Boolean] =
        for {
          v        <- UIO(world.pointLight.position - pt)
          distance <- v.norm
          vNorm    <- v.normalized.orDie
          xs       <- intersections(world, Ray(pt, vNorm))
          hit      <- rayModule.hit(xs)
        } yield hit.exists(i => i.t > 0 && i.t < distance)
    }
  }

  object > extends WorldTopologyModule.Service[WorldTopologyModule] {
    override def intersections(world: World, ray: Ray): ZIO[WorldTopologyModule, Nothing, List[Intersection]] =
      ZIO.accessM(_.worldTopologyModule.intersections(world, ray))
    override def isShadowed(world: World, pt: Pt): ZIO[WorldTopologyModule, Nothing, Boolean] =
      ZIO.accessM(_.worldTopologyModule.isShadowed(world, pt))

  }

}