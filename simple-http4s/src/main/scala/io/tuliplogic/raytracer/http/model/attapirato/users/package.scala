package io.tuliplogic.raytracer.http.model.attapirato

import zio.Has

package object users {
  type UsersRepo = Has[UsersRepo.Service]
}
