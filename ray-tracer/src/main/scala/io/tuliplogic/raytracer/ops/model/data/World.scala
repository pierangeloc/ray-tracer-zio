package io.tuliplogic.raytracer.ops.model.data

import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape}

case class World(pointLight: PointLight, objects: List[Shape])
