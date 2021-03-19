package io.tuliplogic.raytracer.ops.programs

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.geometry.matrix.matrixModule
import io.tuliplogic.raytracer.ops.model.data.rayModule
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import io.tuliplogic.raytracer.ops.model.modules.{cameraModule, lightDiffusionModule, lightReflectionModule, normalReflectModule, phongReflectionModule, rasteringModule, worldHitCompsModule, worldModule, worldTopologyModule}
import io.tuliplogic.raytracer.ops.programs.SimpleWorld.ULayer
import zio.{Layer, ZLayer}
import zio.magic._

object layers {
  val worldM: ULayer[ATModule, WorldModule] = ZLayer.fromSomeMagic[ATModule, WorldModule](
    worldTopologyModule.live,
    worldHitCompsModule.live,
    phongReflectionModule.live,
    rayModule.live,
    normalReflectModule.live,
    lightDiffusionModule.live,
    lightReflectionModule.live,
    worldModule.live
  )


  val rasteringM: ULayer[ATModule, RasteringModule] =
    ZLayer.fromSomeMagic[ATModule, RasteringModule](
      rasteringModule.chunkRasteringModule,
      worldModule.live,
      cameraModule.live,
      worldTopologyModule.live,
      worldHitCompsModule.live,
      phongReflectionModule.live,
      rayModule.live,
      normalReflectModule.live,
      lightDiffusionModule.live,
      lightReflectionModule.live
    )

  val atM: Layer[Nothing, ATModule] =  matrixModule.breezeLive >>>  aTModule.live
}
