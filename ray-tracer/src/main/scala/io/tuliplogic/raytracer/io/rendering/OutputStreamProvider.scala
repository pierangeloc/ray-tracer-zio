package io.tuliplogic.raytracer.io.rendering

import java.io.OutputStream

import io.tuliplogic.raytracer.errors.IOError
import zio.ZIO

trait OutputStreamProvider {}

object OutputStreamProvider {
  trait Service[R] {
    def outputStream: ZIO[R, IOError, OutputStream]
  }
}
