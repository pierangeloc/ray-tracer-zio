package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, RayTracerError}
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.drawing.{Camera, Pattern, Renderer, ViewTransform, World}
import io.tuliplogic.raytracer.ops.model.SceneObject.{Plane, PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{CameraModule, Canvas, Color, Material, NormalReflectModule, PhongReflectionModule, RayModule, SceneObject, WorldModule}
import io.tuliplogic.raytracer.ops.rendering.{CanvasRenderer, canvasRendering}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{App, UIO, ZIO, console}
import zio.ZEnv


object Chapter9World extends App {
  val canvasFile    = "/tmp/nioexp/chapter-9-three-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(-10, 5, -10)
  val cameraFrom    = Pt(0, 1.5, -5)
  val cameraTo      = Pt(0, 1, 0)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer
          with FullModules
          with ATModule.Live with MatrixModule.BreezeMatrixModule with CameraModule.Live with WorldModule.Live with Console.Live with Clock.Live with Blocking.Live {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  //TODO: make a DSL to build a world, this is too painful

  val world = for {
    defaultMat <- Material.default
    idTf       <- ATModule.>.id
    floorMat   <- UIO(defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.9, 0.9), idTf), specular = 0))
    floorS     <- Plane.canonical.map(_.copy(material = floorMat)) //grey, matte

    leftWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    leftWallTf3 <- ATModule.>.rotateY(-math.Pi / 4)
    leftWallTf4 <- ATModule.>.translate(0, 0, 5)
    leftWallTf  <- ATModule.>.compose(leftWallTf2, leftWallTf3).flatMap(ATModule.>.compose(_, leftWallTf4))
    leftWallS   <- UIO(Plane(leftWallTf, floorMat))

    rightWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    rightWallTf3 <- ATModule.>.rotateY(math.Pi / 4)
    rightWallTf4 <- ATModule.>.translate(0, 0, 5)
    rightWallTf  <- ATModule.>.compose(rightWallTf2, rightWallTf3).flatMap(ATModule.>.compose(_, rightWallTf4))
    rightWallS   <- UIO(Plane(rightWallTf, floorMat))

    s1Tf <- ATModule.>.translate(-0.5, 1.2, 0.5)
    s1   <- UIO(Sphere(s1Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.1, 1, 0.5), idTf), diffuse = 0.7, specular = 0.3)))

    s2Tf1 <- ATModule.>.scale(0.5, 0.5, 0.5)
    s2Tf2 <- ATModule.>.translate(1.5, 0.5, -0.5)
    s2Tf  <- ATModule.>.compose(s2Tf2, s2Tf1)
    s2    <- UIO(Sphere(s2Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.5, 1, 0.1), idTf), diffuse = 0.7, specular = 0.3)))

    s3Tf1 <- ATModule.>.scale(0.33, 0.33, 0.33)
    s3Tf2 <- ATModule.>.translate(-1.5, 0.33, -0.75)
    s3Tf  <- ATModule.>.compose(s3Tf2, s3Tf1)
    s3    <- UIO(Sphere(s3Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.8, 0.1), idTf), diffuse = 0.7, specular = 0.3)))
  } yield World(PointLight(lightPosition, Color.white), List[SceneObject](s1, s2, s3, floorS, rightWallS, leftWallS))

  val camera: ZIO[ATModule with MatrixModule, AlgebraicError, Camera] = for {
    cameraTf <- ViewTransform(cameraFrom, cameraTo, cameraUp).tf
  } yield Camera(hRes, vRes, math.Pi / 3, cameraTf)

  val program: ZIO[CanvasRenderer with ATModule with MatrixModule with CameraModule with WorldModule, RayTracerError, Unit] =
    for {
      canvas <- Canvas.create(hRes, vRes)
      wrld   <- world
      cam    <- camera
      _ <- Renderer.render(cam, wrld).flattenChunks.foreach {
        case (px, py, color) =>
          canvas.update(px, py, color)
      }
      _ <- canvasRendering.render(canvas, 255)
    } yield ()

}
