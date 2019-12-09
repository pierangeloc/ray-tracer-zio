package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.HitComps
import zio.{Ref, RefM, UIO, ZIO}

/**
  * Determines how the world gets reflected
  */
trait WorldReflectionModule {
  val worldReflectionModule: WorldReflectionModule.Service[Any]
}

object WorldReflectionModule {
  trait Service[R] {

    /**
      * Determines the color due to the reflection of the world on the hit point
      */
    def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[R, RayTracerError, Color]
  }

  trait Live extends WorldReflectionModule {
    val worldModule: WorldModule.Service[Any]

    val worldReflectionModule = new WorldReflectionModule.Service[Any] {
      def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]):
        ZIO[Any, RayTracerError, Color] =
        if (hitComps.shape.material.reflective == 0) {
          UIO(Color.black)
        } else for {
          rem <- remaining.get
          res <- if (rem <= 0) UIO.succeed(Color.black) else {
            val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
            remaining.update(_ - 1) *> worldModule.colorForRay(world, reflRay, remaining).map(c =>
              c * hitComps.shape.material.reflective
            )
          }
        } yield res
    }
  }

  trait NoReflectionModule extends WorldReflectionModule {
    val worldReflectionModule = new WorldReflectionModule.Service[Any] {
      def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] = UIO.succeed(Color.black)
    }
  }

  object > extends WorldReflectionModule.Service[WorldReflectionModule] {
    override def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[WorldReflectionModule, RayTracerError, Color] =
      ZIO.accessM(_.worldReflectionModule.reflectedColor(world, hitComps, remaining))
  }
}
