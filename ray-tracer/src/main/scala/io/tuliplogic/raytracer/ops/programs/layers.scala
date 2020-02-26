package io.tuliplogic.raytracer.ops.programs

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.geometry.matrix.matrixModule
import io.tuliplogic.raytracer.ops.model.data.rayModule
import io.tuliplogic.raytracer.ops.model.modules.lightReflectionModule.LightReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.PhongReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.model.modules.worldHitCompsModule.WorldHitCompsModule
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import io.tuliplogic.raytracer.ops.model.modules.{cameraModule, lightDiffusionModule, lightReflectionModule, normalReflectModule, phongReflectionModule, rasteringModule, worldHitCompsModule, worldModule, worldTopologyModule}
import io.tuliplogic.raytracer.ops.model.modules.worldTopologyModule.WorldTopologyModule
import io.tuliplogic.raytracer.ops.programs.SimpleWorld.ULayer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock

object layers {
  val clockAndBlocking: ZLayer.NoDeps[Nothing, Blocking with Clock] =
    Blocking.live ++ Clock.live

  val cSerializerM: ULayer[Blocking with Clock, CanvasSerializer] =
    canvasSerializer.pNGCanvasSerializer

  val topologyM: ULayer[ATModule, WorldTopologyModule] =
    rayModule.live >>> worldTopologyModule.live

  val hitCompsM: ULayer[ATModule, WorldHitCompsModule] =
    normalReflectModule.live >>> worldHitCompsModule.live

  val lightReflectionM: ULayer[ATModule, LightReflectionModule] =
    normalReflectModule.live >>> lightReflectionModule.live

  val phongM:  ULayer[ATModule, PhongReflectionModule] =
    (lightDiffusionModule.live ++ lightReflectionM  ++ ZLayer.requires[ATModule]) >>> phongReflectionModule.live

  val worldM: ULayer[ATModule, WorldModule] = (
    topologyM ++
      hitCompsM ++
      phongM
    ) >>> worldModule.live


  val rasteringM: ZLayer[ATModule, Nothing, RasteringModule] = (worldM ++ cameraModule.live) >>> rasteringModule.chunkRasteringModule

  val atM: ZLayer.NoDeps[Nothing, ATModule] =  matrixModule.breezeLive >>>  aTModule.live
}
