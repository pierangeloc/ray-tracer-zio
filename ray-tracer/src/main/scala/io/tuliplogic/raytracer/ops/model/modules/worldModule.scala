package io.tuliplogic.raytracer.ops.model.modules

import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.PhongReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.worldHitCompsModule.WorldHitCompsModule
import io.tuliplogic.raytracer.ops.model.modules.worldReflectionModule.WorldReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.worldRefractionModule.WorldRefractionModule
import io.tuliplogic.raytracer.ops.model.modules.worldTopologyModule.WorldTopologyModule
import zio.interop.catz._
import zio.{Has, IO, Ref, UIO, ZIO, ZLayer}

object worldModule {
  trait Service {
    /**
      * Determines the full color for a ray that hits the world.
      * This is the main method that should be called by a renderer
      */
    def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): IO[RayTracerError, Color]
  }

  type WorldModule = Has[Service]

//  val live: ZLayer[WorldTopologyModule with WorldHitCompsModule with PhongReflectionModule with WorldReflectionModule with WorldRefractionModule, Nothing, WorldModule] =
  //observation: the type inference doesn't work if I put the types in ZLayer and not on fromServices. Try with a minimal example
  val live: ZLayer[WorldTopologyModule with WorldHitCompsModule with PhongReflectionModule with WorldReflectionModule with WorldRefractionModule, Nothing, WorldModule] =
    ZLayer.fromServices[worldTopologyModule.Service, worldHitCompsModule.Service, phongReflectionModule.Service, worldReflectionModule.Service, worldRefractionModule.Service, WorldModule] {
    (worldTopology: worldTopologyModule.Service,
     worldHitComps: worldHitCompsModule.Service, //TODO: remove
     phongReflection: phongReflectionModule.Service,
     worldReflection: worldReflectionModule.Service,
     worldRefraction: worldRefractionModule.Service
    ) => Has(new Service {
      def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] =
        for {
          intersections <- worldTopology.intersections(world, ray)
          maybeHitComps <- intersections.find(_.t > 0).traverse(worldHitComps.hitComps(ray, _, intersections))
          color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) {
              hc =>
                for {
                  shadowed <- worldTopology.isShadowed(world, hc.overPoint)
                    ((c, reflectedColor), refractedColor)    <-
                      (phongReflection.lighting(world.pointLight, hc, shadowed).map(_.toColor) zipPar
                        worldReflection.reflectedColor(world, hc, remaining)) zipPar
                        worldRefraction.refractedColor(world, hc, remaining)
                } yield {
                  if (hc.shape.material.reflective > 0 && hc.shape.material.transparency > 0) {
                    val reflectance = hc.reflectance
                    c + reflectedColor * reflectance + refractedColor * (1 - reflectance)
                  } else {
                    c + reflectedColor + refractedColor
                  }
                }
          }
        } yield color
    })
  }

  def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[WorldModule, RayTracerError, Color] =
    ZIO.accessM(_.get.colorForRay(world, ray, remaining))
}