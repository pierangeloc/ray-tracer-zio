package io.tuliplogic.raytracer.ops.programs

import io.tuliplogic.raytracer.ops.model.{LightDiffusionModule, LightReflectionModule, NormalReflectModule, PhongReflectionModule, RayModule, WorldReflectionModule, WorldRefractionModule}

/**
 * 
 * ray-tracer-zio - 28/10/2019
 * Created with ♥ in Amsterdam
 */
trait FullModules
  extends NormalReflectModule.Live
    with LightDiffusionModule.Live
    with LightReflectionModule.Live
    with PhongReflectionModule.Live
    with RayModule.Live
    with WorldReflectionModule.Live
    with WorldRefractionModule.Live
