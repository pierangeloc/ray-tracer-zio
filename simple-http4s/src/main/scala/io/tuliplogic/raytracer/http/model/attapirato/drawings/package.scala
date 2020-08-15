package io.tuliplogic.raytracer.http.model.attapirato

import zio.Has

package object drawings {
  type ScenesRepo = Has[ScenesRepo.Service]
}
