package io.tuliplogic.raytracer.ops.drawing

  import io.tuliplogic.raytracer.ops.model.SceneObject
  import io.tuliplogic.raytracer.ops.model.SceneObject.PointLight

  import scala.Predef.{any2stringadd => _}

case class World(pointLight: PointLight, objects: List[SceneObject])
