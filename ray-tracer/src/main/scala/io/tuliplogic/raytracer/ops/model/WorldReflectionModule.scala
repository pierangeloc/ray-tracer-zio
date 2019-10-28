package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.drawing.World
import io.tuliplogic.raytracer.ops.model.PhongReflectionModule.HitComps
import zio.{UIO, ZIO}

/**
  * Determines how the world gets reflected
  */
trait WorldReflectionModule {
  val worldReflectionModule: WorldReflectionModule.Service[Any]
}

object WorldReflectionModule {
  trait Service[R] {

    //TODO: see how to manage the `remaining` with Ref
    /**
      * Determines the color due to the reflection of the world on the hit point
      */
    def reflectedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[R, RayTracerError, Color]

  }

  trait Live extends WorldReflectionModule {
    val worldModule: WorldModule.Service[Any]

    val worldReflectionModule = new WorldReflectionModule.Service[Any] {
      def reflectedColor(world: World, hitComps: HitComps, remaining: Int): ZIO[Any, RayTracerError, Color] =
        if (hitComps.obj.material.reflective == 0) {
          UIO(Color.black)
        } else {
          val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
          worldModule.colorForRay(world, reflRay, remaining).map(c =>
            c * hitComps.obj.material.reflective
          )
        }
    }
  }

  trait NoWorldReflectionModule extends WorldReflectionModule {
    val worldReflectionModule = new WorldReflectionModule.Service[Any] {
      def reflectedColor(world: World, hitComps: HitComps, remaining: Int): ZIO[Any, RayTracerError, Color] = UIO.succeed(Color.black)
    }
  }

  object > extends WorldReflectionModule.Service[WorldReflectionModule] {
    override def reflectedColor(world: World, hitComps: HitComps, remaining: Int): ZIO[WorldReflectionModule, RayTracerError, Color] =
      ZIO.accessM(_.worldReflectionModule.reflectedColor(world, hitComps, remaining))
  }
}
