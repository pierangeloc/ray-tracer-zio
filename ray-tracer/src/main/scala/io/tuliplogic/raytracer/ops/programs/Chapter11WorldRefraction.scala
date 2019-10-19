package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, RayTracerError}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
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
import zio.ZEnv


object Chapter11WorldRefraction extends App {
  val canvasFile    = "/tmp/nioexp/chapter-11-refractive-spheres" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(5, 5, 2)
  val cameraFrom    = Pt(0, 10, 0)
  val cameraTo      = Pt.origin
  val cameraUp      = Vec(0, 0, 1)

//  val (hRes, vRes) = (640, 480)
  val (hRes, vRes) = (100, 50)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasRenderer.PPMCanvasRenderer with RichRayOperations.Live with Blocking.Live with MatrixModule.BreezeMatrixModule with Console.Live with Clock.Live
        with AffineTransformationOps.BreezeMatrixOps$ {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  //TODO: make a DSL to build a world, this is too painful

  val world: ZIO[AffineTransformationOps, AlgebraicError, World] = for {
    mat      <- Material.default
    idTf     <- AffineTransformation.id
    planeMat <- UIO(mat.copy(pattern = Pattern.Checker(Color(0.1, 0.1, 0.1), Color(0.3, 0.3, 0.3), idTf), specular = 0, reflective = 0.1))
    translate4DownY <- AffineTransformation.translate(0, -4, 0)
    floorS <- Plane.canonical.map(_.copy(material = planeMat, transformation = translate4DownY))

    s1Tf <- AffineTransformation.scale(4, 4, 4)
    glass <- Material.glass
    s1 <- Sphere.unit.map(_.copy(transformation = s1Tf, material = glass))

    s2Tf <- AffineTransformation.scale(1, 2, 2)
    air <- Material.air
    s2 <- Sphere.unitGlass.map(s => s.copy(transformation = s2Tf, material = air))

    s3Tf <- AffineTransformation.scale(1, 1, 1)
    s3    <- Sphere.unitGlass.map(_.copy(transformation = s3Tf, material = glass))

  } yield World(PointLight(lightPosition, Color.white), List[SceneObject](s1,
    s2, s3,
    floorS))

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
