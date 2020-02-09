package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.data.rayModule.RayModule
import io.tuliplogic.raytracer.ops.model.data.{Intersection, Ray, World, rayModule}
import zio.{Has, UIO, ZIO, ZLayer}

/**
 * 
 * This module provides capabilities to deal purely with the topological structure of our space, i.e. it doesn't bother about
 * materials and the like
 */
object worldTopologyModule {
  trait Service {
    /**
      * Provides the list of intersections between a Ray and the world
      */
    def intersections(world: World, ray: Ray): UIO[List[Intersection]]

    /**
      * Determines if the LOS of a point to the light source is shadowed by an object of the world
      */
    def isShadowed(world: World, pt: Pt): UIO[Boolean]
  }

  type WorldTopologyModule = Has[Service]

  val live: ZLayer[RayModule, Nothing, WorldTopologyModule] =
    ZLayer.fromService[rayModule.Service, Nothing, WorldTopologyModule] { rayModuleSvc =>
      Has(new Service {

        import Ordering.Double.TotalOrdering
        def intersections(world: World, ray: Ray): UIO[List[Intersection]] =
          ZIO.traverse(world.objects)(rayModuleSvc.intersect(ray, _)).map(_.flatten.sortBy(_.t))

        def isShadowed(world: World, pt: Pt): UIO[Boolean] =
          for {
            v        <- UIO(world.pointLight.position - pt)
            distance <- v.norm
            vNorm    <- v.normalized.orDie
            xs       <- intersections(world, Ray(pt, vNorm))
            hit      <- rayModuleSvc.hit(xs)
          } yield hit.exists(i => i.t > 0 && i.t < distance)
      })
    }

  def intersections(world: World, ray: Ray): ZIO[WorldTopologyModule, Nothing, List[Intersection]] =
      ZIO.accessM(_.get.intersections(world, ray))
  def isShadowed(world: World, pt: Pt): ZIO[WorldTopologyModule, Nothing, Boolean] =
      ZIO.accessM(_.get.isShadowed(world, pt))
}