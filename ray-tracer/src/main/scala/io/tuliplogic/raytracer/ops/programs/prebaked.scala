//package io.tuliplogic.raytracer.ops.programs
//
//import io.tuliplogic.raytracer.geometry.affine.aTModule
//import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
//import io.tuliplogic.raytracer.geometry.matrix.matrixModule
//import zio.ZLayer
//import zio.blocking.Blocking
//import zio.clock.Clock
//import zio.console.Console
//
///**
// * The minimal set of modules required for the ray tracer to work and produce ppm files
// */
//
//
//object prebaked {
//
//  val basicModules: ZLayer.NoDeps[Nothing, ATModule] = matrixModule.breezeLive >>> aTModule.live
//}
//trait BasicModules
//  extends NormalReflectModule.Live
//  with RayModule.Live
//  with ATModule.Live
//  with MatrixModule.BreezeLive
//  with WorldModule.Live
//  with WorldTopologyModule.Live
//  with WorldHitCompsModule.Live
//  with CameraModule.Live
//  with RasteringModule.ChunkRasteringModule
//  with Blocking.live
//  with Clock.Live
//  with Console.Live
//
//trait FullModules
//  extends BasicModules
//  with FullRenderingModules
//
//trait FullRenderingModules
//  extends PhongReflectionModule.Live
//  with LightDiffusionModule.Live
//  with LightReflectionModule.Live
//  with WorldReflectionModule.NoReflectionModule
//  with WorldRefractionModule.NoRefractionModule
//
//
//trait BlackWhiteColorModules extends
//  PhongReflectionModule.BlackWhite
//  with WorldReflectionModule.NoReflectionModule
//  with WorldRefractionModule.NoRefractionModule
//
//trait NoReflectionModules extends
//  PhongReflectionModule.Live
//  with LightDiffusionModule.Live
//  with LightReflectionModule.NoReflection
//  with WorldRefractionModule.NoRefractionModule
//  with WorldReflectionModule.NoReflectionModule
//
//trait ReflectionModules extends
//  PhongReflectionModule.Live
//  with LightDiffusionModule.Live
//  with LightReflectionModule.Live
//  with WorldRefractionModule.NoRefractionModule
//  with WorldReflectionModule.NoReflectionModule
//
//trait WorldReflectionModules extends
//  PhongReflectionModule.Live
//  with LightDiffusionModule.Live
//  with LightReflectionModule.Live
//  with WorldReflectionModule.Live
//  with WorldRefractionModule.NoRefractionModule
//
//trait VerySimpleModules
//  extends BasicModules
//  with BlackWhiteColorModules
//
//trait VerySimpleModulesColored
//  extends BasicModules
//  with NoReflectionModules
//
//trait SimpleModulesWorldReflection
//  extends BasicModules
//  with WorldReflectionModules
//
//trait SimpleModulesNoWorldReflection
//  extends BasicModules
//    with ReflectionModules