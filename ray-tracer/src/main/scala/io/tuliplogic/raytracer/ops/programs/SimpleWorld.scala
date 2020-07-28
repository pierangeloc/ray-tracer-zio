package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{ATError, RayTracerError}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, World}
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio.{ExitCode, _}
import zio.clock.Clock

object SimpleWorld extends App{
  val cameraFrom = Pt(2, 2, -18)
  val cameraTo = Pt.origin
  val cameraUp = Vec.uy
  val hRes = 640
  val vRes = 480
  val canvasFile    = "ppm/simple-world-" + System.currentTimeMillis
  type ULayer[A, B <: Has[_]] = ZLayer[A, Nothing, B]

  val world: ZIO[ATModule, ATError, World] = for {
    defaultMat <- Material.default
    s1         <- Sphere.make(Pt.origin, 3, defaultMat)
    s2         <- Sphere.make(Pt(0, 0, -5), 0.3, defaultMat)
    light      <- UIO(PointLight(Pt(0, 0, -15), Color.white))
  } yield World(light, List[Shape](s1, s2))

  def program(viewFrom: Pt, path: Path): ZIO[CanvasSerializer with RasteringModule with ATModule with Clock, RayTracerError, Unit] = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, viewFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- canvasSerializer.serializeToFile(canvas, 255, path)
  } yield ()

  import layers._

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZIO.foreach(-18 to -6)(z => program(Pt(2, 2, z.toDouble), Paths.get(s"$canvasFile-$z.ppm"))
      .provideLayer(cSerializerM ++ (atM >>> rasteringM) ++ atM++ ZLayer.requires[Clock])
    ).timed.foldM(err =>
    console.putStrLn(s"Execution failed with: $err").as(ExitCode.success),
    { case (duration, _) => console.putStrLn(s"rendering took ${duration.toMillis} ms") *> UIO.succeed(ExitCode.failure) }
  )

}
