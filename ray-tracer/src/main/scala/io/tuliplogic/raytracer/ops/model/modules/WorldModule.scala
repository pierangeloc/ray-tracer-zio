package io.tuliplogic.raytracer.ops.model.modules

import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.HitComps
import zio.interop.catz._
import zio.macros.annotation.mockable
import zio.{IO, Ref, UIO, ZIO}

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
    def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[R, RayTracerError, Color]
  }

  trait Live extends WorldModule {
    val worldTopologyModule: WorldTopologyModule.Service[Any]
    val worldHitCompsModule: WorldHitCompsModule.Service[Any]
    val phongReflectionModule: PhongReflectionModule.Service[Any]
    val worldReflectionModule: WorldReflectionModule.Service[Any]
    val worldRefractionModule: WorldRefractionModule.Service[Any]

    override val worldModule: Service[Any] = new Service[Any] {

      def schlick(hitComps: HitComps) = {
        val cos = if (hitComps.n1 <= hitComps.n2)
          hitComps.eyeV dot hitComps.normalV
          else {
            val c = hitComps.eyeV dot hitComps.normalV
            val n = hitComps.n1 / hitComps.n2
            val sin2t = n * n / (1 - c * c)
            if (sin2t > 1)
              1
            else {
              math.sqrt(1 - sin2t)
            }
          }
        val r0: Double = (hitComps.n1 - hitComps.n2) / (hitComps.n1 * hitComps.n2) * (hitComps.n1 - hitComps.n2) / (hitComps.n1 * hitComps.n2)
        r0 + (1 - r0) * (1 - cos) * (1 - cos) * (1 - cos) * (1 - cos) * (1 - cos)
      }

      def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] =
        for {
          intersections <- worldTopologyModule.intersections(world, ray)
          maybeHitComps <- intersections.find(_.t > 0).traverse(worldHitCompsModule.hitComps(ray, _, intersections))
          rem           <- remaining.update(_ - 1)
          color <- if (rem <= 0) UIO.succeed(Color.black)
            else maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) {
              hc =>
                for {
                  shadowed <- worldTopologyModule.isShadowed(world, hc.overPoint)
                    ((c, reflectedColor), refractedColor)    <-
                      (phongReflectionModule.lighting(world.pointLight, hc, shadowed).map(_.toColor) zipPar
                        worldReflectionModule.reflectedColor(world, hc, remaining)) zipPar
                        worldRefractionModule.refractedColor(world, hc, remaining)
                } yield {
                  if (hc.shape.material.reflective > 0 && hc.shape.material.transparency > 0) {
                    val reflectance = schlick(hc)
                    c + reflectedColor * reflectance + refractedColor * (1 - reflectance)

                  } else {
                    c + reflectedColor + refractedColor
                  }
                }
          }
        } yield color
    }
  }

  object > extends WorldModule.Service[WorldModule] {
    override def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[WorldModule, RayTracerError, Color] =
      ZIO.accessM(_.worldModule.colorForRay(world, ray, remaining))
  }
}