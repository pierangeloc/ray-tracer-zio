package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.drawing.{Pattern, World}
import io.tuliplogic.raytracer.ops.model.SceneObject.{Plane, PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.{CameraModule, Color, Material, RasteringModule, SceneObject, WorldModule}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.blocking.Blocking
import zio.console.Console
import zio.{App, UIO, ZEnv, ZIO, console}

object Chapter10World extends App {
  val canvasFile    = "ppm/chapter-10-two-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(10, 4, 2)
  val cameraFrom    = Pt(15, 4, 0)
  val cameraTo      = Pt(0, 4, 12)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)
//  val (hRes, vRes) = (100, 50)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with FullModules with ATModule.Live with MatrixModule.BreezeMatrixModule
          with WorldModule.Live with CameraModule.Live with RasteringModule.ChunkRasteringModule
          with Blocking.Live with Console.Live
         {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  //TODO: make a DSL to build a world, this is too painful

  val world: ZIO[ATModule, AlgebraicError, World] = for {
    mat      <- Material.default
    idTf     <- ATModule.>.id
    floorMat <- UIO(mat.copy(pattern = Pattern.Uniform(Color(1, 0.9, 0.9), idTf), specular = 0))
    floorS   <- Plane.canonical.map(_.copy(material = floorMat)) //grey, matte

    leftWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    leftWallTf3 <- ATModule.>.rotateY(-math.Pi / 2)
    leftWallTf4 <- ATModule.>.translate(-10, 0, 0)
    leftWallTf  <- ATModule.>.compose(leftWallTf2, leftWallTf3).flatMap(ATModule.>.compose(_, leftWallTf4))
    leftWallS   <- UIO(Plane(leftWallTf, floorMat))

    s1Tf1 <- ATModule.>.translate(5, 2, 5)
    s1Tf2 <- ATModule.>.scale(2, 2, 2)
    s1Tf  <- ATModule.>.compose(s1Tf2, s1Tf1)
    s1    <- UIO(Sphere(s1Tf, mat.copy(pattern = Pattern.Striped(Color.black, Color.white, idTf), diffuse = 0.7, specular = 0.3)))

    s2Tf1 <- ATModule.>.translate(10, 4, 8)
    s2Tf2 <- ATModule.>.scale(3, 3, 3)
    s2Tf  <- ATModule.>.compose(s2Tf2, s2Tf1)
    s2    <- UIO(Sphere(s2Tf, mat.copy(pattern = Pattern.Striped(Color.blue, Color.red, idTf), diffuse = 0.7, specular = 0.3)))

  } yield World(PointLight(lightPosition, Color.white), List[SceneObject](s1, s2, floorS, leftWallS))

  val program = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- CanvasSerializer.>.render(canvas, 255)
  } yield ()
}
