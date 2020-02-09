package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.commons.errors.BusinessError.GenericError
import io.tuliplogic.raytracer.commons.errors.IOError.HttpError
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.http.model.Shape.{Plane, Sphere}
import io.tuliplogic.raytracer.ops.model.data
import io.tuliplogic.raytracer.ops.model.data.{Color, Scene, World}
import zio.console.Console
import zio.{UIO, ZIO}

object Http2World {
  case class SceneBundle(
      world: World,
      viewFrom: Pt,
      viewTo: Pt,
      viewUp: Vec,
      visualAngleRad: Double,
      hRes: Int,
      vRes: Int
  )

  private def viewFrom(httpCamera: Camera): Pt =
    Pt(httpCamera.fromX, httpCamera.fromY, httpCamera.fromZ)
  private def viewTo(httpCamera: Camera): Pt = Pt(httpCamera.toX, httpCamera.toY, httpCamera.toZ)
  private def upVec(httpCamera: Camera): Vec = Vec(httpCamera.upX, httpCamera.upY, httpCamera.upZ)

  private def pattern(pattern: Pattern): ZIO[ATModule, GenericError, data.Pattern] = pattern match {
    case Pattern.Striped(c1, c2, stripSize) =>
      for {
        col1 <- Color.fromHex(c1)
        col2 <- Color.fromHex(c2)
        tf   <- aTModule.scale(stripSize, 1, 1)
      } yield data.Pattern.Striped(col1, col2, tf)

    case Pattern.Checker(c1, c2, size) =>
      for {
        col1 <- Color.fromHex(c1)
        col2 <- Color.fromHex(c2)
        tf   <- aTModule.scale(size, size, size)
      } yield data.Pattern.Checker(col1, col2, tf)

    case Pattern.Uniform(c) =>
      for {
        col <- Color.fromHex(c)
        tf  <- aTModule.id
      } yield data.Pattern.Uniform(col, tf)

    case Pattern.GradientX(c1, c2) => for {
      col1 <- Color.fromHex(c1)
      col2 <- Color.fromHex(c2)
      tf  <- aTModule.id
    } yield data.Pattern.GradientX(col1, col2, tf)
  }

  private def material(mat: Material): ZIO[ATModule with Console, GenericError, data.Material] = mat match {
    case m @ Material(p, ambient, diffuse, specular, shininess, reflective, transparency, refractionIndex) =>
      for {
        _ <- zio.console.putStrLn(s"Creating material from $m")
        p <- pattern(p)
        _ <- zio.console.putStrLn(s"Created pattern $p for material")
      } yield
        data.Material(
          p,
          ambient.getOrElse(0),
          diffuse.getOrElse(0),
          specular.getOrElse(0),
          shininess.getOrElse(0),
          reflective.getOrElse(0),
          transparency.getOrElse(0),
          refractionIndex.getOrElse(1)
        )
  }

  private def httpShape2Shape(s: Shape): ZIO[ATModule with Console, RayTracerError with Product with Serializable, data.Scene.Shape] = s match {
    case p @ Plane(m, rotateX, rotateY, rotateZ, translateX, translateY, translateZ) =>
      for {
        _   <- zio.console.putStrLn(s"creating plane from $p")
        mat <- material(m)
        plane <- data.Scene.Plane.make(
          rotateX.getOrElse(0),
          rotateY.getOrElse(0),
          rotateZ.getOrElse(0),
          Pt(translateX.getOrElse(0), translateY.getOrElse(0), translateZ.getOrElse(0)),
          mat)
      } yield plane

    case sph @ Sphere(m, centerX, centerY, centerZ, radius) =>
      for {
        _   <- zio.console.putStrLn(s"creating sphere from $sph")
        mat <- material(m)
        s <- data.Scene.Sphere.make(
          Pt(centerX, centerY, centerZ),
          radius,
          mat
        )
      } yield s
  }

  def httpScene2World(httpScene: Scene): ZIO[ATModule with Console, HttpError, SceneBundle] = (
    for {
      worldShapes     <- ZIO.traverse(httpScene.shapes)(httpShape2Shape).mapError(e => HttpError(e.getMessage))
      pointLightColor <- Color.fromHex(httpScene.pointLight.color).mapError(e => HttpError(e.getMessage))
      pointLight      <- UIO(data.Scene.PointLight(Pt(httpScene.pointLight.ptX, httpScene.pointLight.ptY, httpScene.pointLight.ptZ), pointLightColor))

    } yield
      SceneBundle(
        World(pointLight, worldShapes),
        viewFrom(httpScene.camera),
        viewTo(httpScene.camera),
        upVec(httpScene.camera),
        math.Pi / 3,
        httpScene.camera.hRes,
        httpScene.camera.vRes
      )
  )

}
