package io.tuliplogic.raytracer.http

import zio.Has

package object drawings {
  type ScenesRepo = Has[ScenesRepo.Service]
  type Scenes = Has[Scenes.Service]
  type PngRenderer = Has[PngRenderer.Service]
}
