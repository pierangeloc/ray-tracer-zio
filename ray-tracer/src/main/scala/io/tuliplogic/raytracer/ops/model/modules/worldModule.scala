package io.tuliplogic.raytracer.ops.model.modules

import cats.implicits._
import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.{HitComps, PhongReflectionModule}
import io.tuliplogic.raytracer.ops.model.modules.worldHitCompsModule.WorldHitCompsModule
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
    def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color]
    def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color]
  }

  type WorldModule = Has[Service]

//  val live: ZLayer[WorldTopologyModule with WorldHitCompsModule with PhongReflectionModule with WorldReflectionModule with WorldRefractionModule, Nothing, WorldModule] =
  //observation: the type inference doesn't work if I put the types in ZLayer and not on fromServices. Try with a minimal example
  val live: ZLayer[WorldTopologyModule with WorldHitCompsModule with PhongReflectionModule, Nothing, WorldModule] =
    ZLayer.fromServices[worldTopologyModule.Service, worldHitCompsModule.Service, phongReflectionModule.Service, worldModule.Service] {
      (worldTopology, worldHitComps, phongReflection) =>
        new Service {
          def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] =
            for {
              intersections <- worldTopology.intersections(world, ray)
              maybeHitComps <- intersections.find(_.t > 0).traverse(worldHitComps.hitComps(ray, _, intersections))
              color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) {
                hc =>
                  for {
                    shadowed <- worldTopology.isShadowed(world, hc.overPoint)
                    ((c, reflectedColor), refractedColor) <-
                      (phongReflection.lighting(world.pointLight, hc, shadowed).map(_.toColor) zipPar
                        reflectedColor(world, hc, remaining)) zipPar
                        refractedColor(world, hc, remaining)
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

          def reflectedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): IO[RayTracerError, Color] =
            if (hitComps.shape.material.reflective == 0) {
              UIO(Color.black)
            } else for {
              rem <- remaining.get
              res <- if (rem <= 0) UIO.succeed(Color.black) else {
                val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
                remaining.update(_ - 1) *> colorForRay(world, reflRay, remaining).map(c =>
                  c * hitComps.shape.material.reflective
                )
              }
            } yield res

          def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] = {
            if (hitComps.shape.material.transparency == 0)
              UIO.succeed(Color.black) // opaque surfaces don't refract
            else for {
              r <- remaining.get
              res <- if (r <= 0) UIO(Color.black) else {
                val nRatio = hitComps.n1 / hitComps.n2
                val cosTheta_i = (hitComps.eyeV dot hitComps.normalV)
                val sin2Theta_t = nRatio * nRatio * (1 - cosTheta_i * cosTheta_i)
                if (sin2Theta_t > 1) UIO.succeed(Color.black) // total internal reflection reached
                val cosTheta_t: Double = math.sqrt(1 - sin2Theta_t)
                val direction: Vec = (hitComps.normalV * (nRatio * cosTheta_i - cosTheta_t)) - (hitComps.eyeV * nRatio)
                val refractedRay = Ray(hitComps.underPoint, direction)
                remaining.update(_ - 1) *> colorForRay(world, refractedRay, remaining).map(_ * hitComps.shape.material.transparency)
              }
            } yield res
          }
        }
    }

  def colorForRay(world: World, ray: Ray, remaining: Ref[Int]): ZIO[WorldModule, RayTracerError, Color] =
    ZIO.accessM(_.get.colorForRay(world, ray, remaining))
}