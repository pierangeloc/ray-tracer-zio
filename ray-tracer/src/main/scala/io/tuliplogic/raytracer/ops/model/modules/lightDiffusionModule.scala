package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.Color
import zio.ZLayer.NoDeps
import zio.{Has, UIO, ZIO, ZLayer}

/**
  * This module computes the diffusion component of the light, which represents how the light hitting a surface gets diffused around
  */
object lightDiffusionModule {
  trait Service {
    def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): UIO[Color]
  }

  type LightDiffusionModule = Has[Service]

  val live: NoDeps[Nothing, Has[Service]] = ZLayer.succeed(
    new Service {
      override def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): ZIO[Any, Nothing, Color] =
        UIO(lightV dot normalV).map { projection =>
          if (projection < 0) Color.black else effectiveColor * materialDiffusion * projection
        }
    }
  )

  val noDiffusion: NoDeps[Nothing, Has[Service]] = ZLayer.succeed(
    new Service {
      override def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): ZIO[Any, Nothing, Color] =
        UIO(Color.black)
    }
  )

}