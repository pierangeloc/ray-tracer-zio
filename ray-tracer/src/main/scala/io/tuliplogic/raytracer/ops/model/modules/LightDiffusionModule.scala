package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.Color
import zio.{UIO, ZIO}

/**
  * This module computes the diffusion component of the light, which represents how the light hitting a surface gets diffused around
  */
trait LightDiffusionModule {
  val lightDiffusionModule: LightDiffusionModule.Service[Any]
}

object LightDiffusionModule {
  trait Service[R] {
    def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): ZIO[R, Nothing, Color]
  }

  trait Live extends LightDiffusionModule {
    val lightDiffusionModule: Service[Any] = new Service[Any] {
      override def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): ZIO[Any, Nothing, Color] =
        UIO(lightV dot normalV).map { projection =>
          if (projection < 0) Color.black else effectiveColor * materialDiffusion * projection
        }
    }
  }

  trait NoDiffusion extends LightDiffusionModule {
    override val lightDiffusionModule: Service[Any] = new Service[Any] {
      override def diffusion(effectiveColor: Color, materialDiffusion: Double, lightV: Vec, normalV: Vec): ZIO[Any, Nothing, Color] =
        UIO(Color.black)
    }
  }

}