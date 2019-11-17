package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, Pattern, World}
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio._

object ComplexColoredWorld extends App{
  val cameraTo = Pt(20, 0, 20)
  val cameraUp = Vec.uy
  val pointLight = PointLight(Pt(20, 10, -15), Color.white)
//  val hRes = 1024
//  val vRes = 768
  val hRes = 640
  val vRes = 480
  val canvasFile    = "ppm/complex-colored-world-" + System.currentTimeMillis

  val floor = for {
    mat     <- Material.default
    scale4  <- ATModule.>.scale(4, 4, 4)
    wallMat <- UIO(mat.copy(pattern = Pattern.Checker(Color(0.9, 0.9, 0.9), Color(0.5, 0.5, 0.5), scale4), specular = 0))
    floorS  <- Plane.canonical.map(_.copy(material = wallMat))
  } yield floorS

  val greenOrangeMaterial = Material.gradient(
    from = Color(240 / 256.0, 121 / 256.0, 49 / 256.0),
    to = Color(145 / 256.0, 179 / 256.0, 87 / 256.0),
  )

  val bigOpaqueSphere = greenOrangeMaterial.flatMap(Sphere.make(Pt(40, 10, 20), 10, _))
//  val bigGlassSphere = Material.glass.flatMap(Sphere.make(Pt(20, 10, 15), 10, _))
  val bigGlassSphere = Material.striped(Color.white, Color.green, 0.2, reflective = 0.9).flatMap(Sphere.make(Pt(20, 10, 15), 10, _))
  val bigReflectiveSphere = Material.uniform(Color.red, reflective = 0.6).flatMap(Sphere.make(Pt(-5, 8, 10), 8, _))

  val world = for {
    f <- floor
    s1 <- bigOpaqueSphere
    s2 <- bigGlassSphere
    s3 <- bigReflectiveSphere

    light      <- UIO(pointLight)
  } yield World(light, List[Shape](s1, s2, s3, f))

  def program(viewFrom: Pt): ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit] = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, viewFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- CanvasSerializer.>.serialize(canvas, 255)
  } yield ()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    ZIO.traverse(-35 to -35)(z => program(Pt(57, 20, z))
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with SimpleModulesNoWorldReflection {
          override def path: Path = Paths.get(s"$canvasFile-$z.ppm")
        }
      }
    ).timed.foldM(err =>
      console.putStrLn(s"Execution failed with: $err").as(1),
      { case (duration, _) => console.putStrLn(s"rendering took ${duration.toMillis} ms") *> UIO.succeed(0) }
    )

}
