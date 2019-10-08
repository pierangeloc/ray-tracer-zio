package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, RayTracerError}
import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.vectorspace.{AffineTransformation, AffineTransformationOps}
import io.tuliplogic.raytracer.ops.drawing.Scene.RichRayOperations
import io.tuliplogic.raytracer.ops.drawing.{Camera, Pattern, Renderer, ViewTransform, World}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{Plane, PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{Canvas, Color, Material, PhongReflection, RayOperations, SpatialEntityOperations}
import io.tuliplogic.raytracer.ops.rendering.{CanvasRenderer, canvasRendering}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{App, UIO, ZIO, console}

object Chapter10World extends App {
  val canvasFile    = "/tmp/nioexp/chapter-10-two-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(10, 4, 2)
  val cameraFrom    = Pt(15, 4, 0)
  val cameraTo      = Pt(0, 4, 12)
  val cameraUp      = Vec(0, 1, 0)

//  val (hRes, vRes) = (640, 480)
  val (hRes, vRes) = (100, 50)

  override def run(args: List[String]): ZIO[Chapter10World.Environment, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer with RichRayOperations.Live with Blocking.Live with MatrixOps.Live with Console.Live with Clock.Live
        with AffineTransformationOps.Live {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  //TODO: make a DSL to build a world, this is too painful

  val world: ZIO[AffineTransformationOps, AlgebraicError, World] = for {
    floorMat <- UIO(Material.default.copy(color = Pattern.Uniform(Color(1, 0.9, 0.9)), specular = 0))
    floorS   <- Plane.canonical.map(_.copy(material = floorMat)) //grey, matte

    s1Tf1 <- AffineTransformation.translate(5, 2, 5)
    s1Tf2 <- AffineTransformation.scale(2, 2, 2)
    s1Tf  <- s1Tf2 >=> s1Tf1
    s1   <- UIO(Sphere(s1Tf, Material.default.copy(color = Pattern.Uniform(Color.red), diffuse = 0.7, specular = 0.3)))

    s2Tf1 <- AffineTransformation.translate(10, 4, 8)
    s2Tf2 <- AffineTransformation.scale(3, 3, 3)
    s2Tf  <- s2Tf2 >=> s2Tf1
    s2    <- UIO(Sphere(s2Tf, Material.default.copy(color = Pattern.Uniform(Color.blue), diffuse = 0.7, specular = 0.3)))

  } yield World(PointLight(lightPosition, Color.white), List[SceneObject](s1, s2, floorS))

  val camera: ZIO[AffineTransformationOps, AlgebraicError, Camera] = for {
    cameraTf <- ViewTransform(cameraFrom, cameraTo, cameraUp).tf
  } yield Camera(hRes, vRes, math.Pi / 2, cameraTf)

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
