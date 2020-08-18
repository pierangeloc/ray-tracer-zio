package io.tuliplogic.raytracer.http

import zio.Has

package object users {
  type UsersRepo = Has[UsersRepo.Service]
  type Users = Has[Users.Service]
}
