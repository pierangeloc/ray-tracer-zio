package io.tuliplogic.raytracer.http

import zio.{App, ExitCode, URIO, console}
import zio.stream._

object Tst extends App {
  val str = Stream.fromIterable(0 to 5).mapM(i => console.putStrLn(s"$i")).schedule(zio.Schedule.spaced(zio.duration.Duration.fromMillis(1000)))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    str.runDrain *> console.putStrLn("Done!").exitCode
}
