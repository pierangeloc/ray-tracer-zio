package io.tuliplogic.raytracer.http.model.attapirato

import zio.{App, ExitCode, UIO, URIO}

object Main extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = UIO(0).exitCode


}
