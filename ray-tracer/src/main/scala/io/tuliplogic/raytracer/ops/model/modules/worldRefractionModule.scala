package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.HitComps
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import zio.{Has, IO, Ref, UIO, ZIO, ZLayer}

object worldRefractionModule {

  trait Service {
    def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color]
  }

  type WorldRefractionModule = Has[Service]

  val live: ZLayer[WorldModule, Nothing, WorldRefractionModule] = ZLayer.fromService[worldModule.Service, WorldRefractionModule]{
    worldModuleSvc =>

    Has(new Service {
      def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] = {

        if (hitComps.shape.material.transparency == 0)
          UIO.succeed(Color.black) // opaque surfaces don't refract
        else for {
          r   <- remaining.get
          res <- if (r <= 0) UIO(Color.black) else {
             val nRatio = hitComps.n1 / hitComps.n2
             val cosTheta_i = (hitComps.eyeV dot hitComps.normalV)
             val sin2Theta_t = nRatio * nRatio * (1 - cosTheta_i * cosTheta_i)
             if (sin2Theta_t > 1) UIO.succeed(Color.black) // total internal reflection reached
             val cosTheta_t: Double = math.sqrt(1 - sin2Theta_t)
             val direction: Vec = (hitComps.normalV * (nRatio * cosTheta_i - cosTheta_t)) - (hitComps.eyeV * nRatio)
             val refractedRay = Ray(hitComps.underPoint, direction)
             remaining.update(_ - 1) *> worldModuleSvc.colorForRay(world, refractedRay, remaining).map(_ * hitComps.shape.material.transparency)
          }
        } yield res
      }
    })
  }

  val noRefraction: ZLayer.NoDeps[Nothing, WorldRefractionModule] = ZLayer.succeed {
    new Service {
      override def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] =
        UIO.succeed(Color.black)
    }
  }

  def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[WorldRefractionModule, RayTracerError, Color] =
    ZIO.accessM(_.get.refractedColor(world, hitComps, remaining))
}