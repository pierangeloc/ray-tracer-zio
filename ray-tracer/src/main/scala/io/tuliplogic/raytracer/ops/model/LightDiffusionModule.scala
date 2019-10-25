package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import zio.{UIO, ZIO}


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