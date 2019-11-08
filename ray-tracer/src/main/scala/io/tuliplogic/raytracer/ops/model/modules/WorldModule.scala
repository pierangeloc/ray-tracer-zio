package io.tuliplogic.raytracer.ops.model.modules

import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import zio.interop.catz._
import zio.macros.annotation.mockable
import zio.{UIO, ZIO}

@mockable
trait WorldModule {
  val worldModule: WorldModule.Service[Any]
}

object WorldModule {
  trait Service[R] {

    /**
      * Determines the full color for a ray that hits the world.
      * This is the main method that should be called by a renderer
      */
    def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[R, RayTracerError, Color]
  }

  trait Live extends WorldModule {
    val normalReflectModule: NormalReflectModule.Service[Any]
    val worldTopologyModule: WorldTopologyModule.Service[Any]
    val worldHitCompsModule: WorldHitCompsModule.Service[Any]
    val worldReflectionModule: WorldReflectionModule.Service[Any]
    val worldRefractionModule: WorldRefractionModule.Service[Any]
    val phongReflectionModule: PhongReflectionModule.Service[Any]

    override val worldModule: Service[Any] = new Service[Any] {
      def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[Any, RayTracerError, Color] =
        for {
          intersections <- worldTopologyModule.intersections(world, ray)
          maybeHitComps <- intersections.find(_.t > 0).traverse(worldHitCompsModule.hitComps(ray, _, intersections))
          color <- maybeHitComps
            .map(hc =>
              for {
                shadowed <- worldTopologyModule.isShadowed(world, hc.overPoint)
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
    override def colorForRay(world: World, ray: Ray, remaining: Int = 10): ZIO[WorldModule, RayTracerError, Color] =
      ZIO.accessM(_.worldModule.colorForRay(world, ray, remaining))
  }
}