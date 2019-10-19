package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, RayTracerError}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.affine.{AffineTransformation, AffineTransformationOps}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.drawing.Scene.RichRayOperations
import io.tuliplogic.raytracer.ops.drawing.{Camera, Pattern, Renderer, ViewTransform, World}
import io.tuliplogic.raytracer.ops.model.{Canvas, Color, Material, PhongReflection, RayOperations, SpatialEntityOperations}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import io.tuliplogic.raytracer.ops.rendering.{canvasRendering, CanvasRenderer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{console, App, UIO, ZIO}
import zio.ZEnv



object Chapter7World extends App {
  val canvasFile    = "/tmp/nioexp/chapter-7-three-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(-10, 5, -10)
  val cameraFrom    = Pt(0, 1.5, -5)
  val cameraTo      = Pt(0, 1, 0)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer with RichRayOperations.Live with Blocking.Live with MatrixModule.BreezeMatrixModule with Console.Live with Clock.Live
        with AffineTransformationOps.BreezeMatrixOps$ {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: $err").as(1), _ => UIO.succeed(0))

  //TODO: make a DSL to build a world, this is too painful

  val world: ZIO[AffineTransformationOps, AlgebraicError, World] = for {
    defaultMat   <- Material.default
    idTf         <- AffineTransformation.id
    mat          <- UIO(defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.9, 0.9), idTf), specular = 0))
    floorTf      <- AffineTransformation.scale(10, 0.01, 10) //very flat ellipsoid
    floorS       <- UIO(Sphere(floorTf, mat)) //grey, matte
    leftWallTf1  <- AffineTransformation.scale(10, 0.01, 10)
    leftWallTf2  <- AffineTransformation.rotateX(math.Pi / 2)
    leftWallTf3  <- AffineTransformation.rotateY(-math.Pi / 4)
    leftWallTf4  <- AffineTransformation.translate(0, 0, 5)
    leftWallTf   <- (leftWallTf1 >=> leftWallTf2).flatMap(_ >=> leftWallTf3).flatMap(_ >=> leftWallTf4)
    leftWallS    <- UIO(Sphere(leftWallTf, mat))
    rightWallTf1 <- AffineTransformation.scale(10, 0.01, 10)
    rightWallTf2 <- AffineTransformation.rotateX(math.Pi / 2)
    rightWallTf3 <- AffineTransformation.rotateY(math.Pi / 4)
    rightWallTf4 <- AffineTransformation.translate(0, 0, 5)
    rightWallTf  <- (rightWallTf1 >=> rightWallTf2).flatMap(_ >=> rightWallTf3).flatMap(_ >=> rightWallTf4)
    rightWallS   <- UIO(Sphere(rightWallTf, mat))
    s1Tf         <- AffineTransformation.translate(-0.5, 1.2, 0.5)
    s1           <- UIO(Sphere(s1Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.1, 1, 0.5), idTf), diffuse = 0.7, specular = 0.3)))
    s2Tf1        <- AffineTransformation.scale(0.5, 0.5, 0.5)
    s2Tf2        <- AffineTransformation.translate(1.5, 0.5, -0.5)
    s2Tf         <- s2Tf2 >=> s2Tf1
    s2           <- UIO(Sphere(s2Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.5, 1, 0.1), idTf), diffuse = 0.7, specular = 0.3)))
    s3Tf1        <- AffineTransformation.scale(0.33, 0.33, 0.33)
    s3Tf2        <- AffineTransformation.translate(-1.5, 0.33, -0.75)
    s3Tf         <- s3Tf2 >=> s3Tf1
    s3           <- UIO(Sphere(s3Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.8, 0.1), idTf), diffuse = 0.7, specular = 0.3)))
  } yield World(PointLight(lightPosition, Color.white), List(s1, s2, s3, floorS, rightWallS, leftWallS))

  val camera: ZIO[AffineTransformationOps, AlgebraicError, Camera] = for {
    cameraTf <- ViewTransform(cameraFrom, cameraTo, cameraUp).tf
  } yield Camera(hRes, vRes, math.Pi / 3, cameraTf)

  val program: ZIO[PhongReflection with SpatialEntityOperations with RayOperations with AffineTransformationOps with CanvasRenderer, RayTracerError, Unit] =
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
