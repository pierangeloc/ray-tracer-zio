package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.drawing.{Pattern, World}
import io.tuliplogic.raytracer.ops.model.SceneObject.{Plane, PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{CameraModule, Color, Material, RasteringModule, SceneObject, WorldModule}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{console, App, UIO, ZEnv, ZIO}

object Chapter9World extends App {
  val canvasFile    = "ppm/chapter-9-three-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(-10, 5, -10)
  val cameraFrom    = Pt(0, 1.5, -5)
  val cameraTo      = Pt(0, 1, 0)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with FullModules with RasteringModule.ChunkRasteringModule with ATModule.Live
        with MatrixModule.BreezeMatrixModule with CameraModule.Live with WorldModule.Live with Console.Live with Clock.Live with Blocking.Live {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  val world = for {
    defaultMat <- Material.default
    idTf       <- ATModule.>.id
    floorMat   <- UIO(defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.9, 0.9), idTf), specular = 0))
    floorS     <- Plane.make(0, 0, 0, Pt.origin, floorMat) //grey, matte
    leftWallS  <- Plane.make(math.Pi / 2, -math.Pi / 4, 0, Pt(0, 0, 5), floorMat)
    rightWallS <- Plane.make(math.Pi / 2, math.Pi / 4, 0, Pt(0, 0, 5), floorMat)
    s1         <- Sphere.make(Pt(-0.5, 1.2, 0.5), 1, defaultMat.copy(pattern = Pattern.Uniform(Color(0.1, 1, 0.5), idTf), diffuse = 0.7, specular = 0.3))
    s2         <- Sphere.make(Pt(1.5, 0.5, -0.5), 0.5, defaultMat.copy(pattern = Pattern.Uniform(Color(0.5, 1, 0.1), idTf), diffuse = 0.7, specular = 0.3))
    s3         <- Sphere.make(Pt(-1.5, 0.33, -0.75), 0.33, defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.8, 0.1), idTf), diffuse = 0.7, specular = 0.3))
  } yield World(PointLight(lightPosition, Color.white), List[SceneObject](s1, s2, s3, floorS, rightWallS, leftWallS))

  val program = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- CanvasSerializer.>.render(canvas, 255)
  } yield ()

}
