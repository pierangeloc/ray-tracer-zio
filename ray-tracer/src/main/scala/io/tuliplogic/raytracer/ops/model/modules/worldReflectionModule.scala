package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.HitComps
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import zio.{Has, IO, Ref, UIO, ZIO, ZLayer}

/**
  * Determines how the world gets reflected
  */
object worldReflectionModule {
  trait Service {
    /**
      * Determines the color due to the reflection of the world on the hit point
      */
    def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color]
  }

  type WorldReflectionModule = Has[Service]

  val live: ZLayer[WorldModule, Nothing, WorldReflectionModule] =
    ZLayer.fromService[worldModule.Service, WorldReflectionModule] { worldModuleSvc =>
      Has(new Service {
        def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color] =
          if (hitComps.shape.material.reflective == 0) {
            UIO(Color.black)
          } else for {
              rem <- remaining.get
              res <- if (rem <= 0) UIO.succeed(Color.black) else {
                val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
                remaining.update(_ - 1) *> worldModuleSvc.colorForRay(world, reflRay, remaining).map(c =>
                  c * hitComps.shape.material.reflective
                )
              }
            } yield res
    })
  }

  val noReflection: ZLayer.NoDeps[Nothing, WorldReflectionModule] = ZLayer.succeed {
    new Service {
      def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] = UIO.succeed(Color.black)
    }
  }

  def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[WorldReflectionModule, RayTracerError, Color] =
    ZIO.accessM(_.get.reflectedColor(world, hitComps, remaining))
}
