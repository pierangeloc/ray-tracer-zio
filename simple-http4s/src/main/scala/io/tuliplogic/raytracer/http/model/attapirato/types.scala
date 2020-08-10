package io.tuliplogic.raytracer.http.model.attapirato

import java.util.UUID

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object types {

  sealed trait AppError extends Throwable {
    val cause: Option[Throwable] = None

    override def getCause: Throwable = cause.orNull
  }

  object AppError {
    case class APIError(code: Int, message: String)                                                   extends AppError
    case class DBError(code: Int, message: String, override val cause: Option[Throwable] = None)      extends AppError
    case class DrawingError(code: Int, message: String, override val cause: Option[Throwable] = None) extends AppError
    case class BootstrapError(code: Int, message: String, override val cause: Option[Throwable] = None) extends AppError
  }

  object user {
    @newtype case class AccessToken(value: NonEmptyString)
    @newtype case class UserId(value: UUID)
    @newtype case class Email(value: EmailValue)
    @newtype case class PasswordHash(value: NonEmptyString)

    case class User(id: UserId, email: Email, password: Option[PasswordHash])
    case class CreateUserCmd(email: Email)
    case class UserCreated(userId: UserId, accessToken: AccessToken)
  }

  object drawing {
    case class Material(
                         pattern: Pattern,
                         ambient: Option[Double], //TODO refine Double > 0 && < 1
                         diffuse: Option[Double], //TODO refine Double > 0 && < 1
                         specular: Option[Double], //TODO refine Double > 0 && < 1 specularity of the surface to the light source
                         shininess: Option[Double], //TODO refine Double > 10 && < 200 shininess of the surface to the light source
                         reflective: Option[Double], //TODO refine Double [0, 1] generic reflectiveness of the surface, of generic rays not only coming from the light sourcex
                         transparency: Option[Double], //TODO refine Double [0, 1] how transparent the material is
                         refractionIndex: Option[Double] //TODO refine Double [0, 1] the material refraction index (for vacuum it's 1)
                       )

    sealed trait Pattern
    object Pattern {
      case class Striped(c1: String, c2: String, stripSize: Double) extends Pattern
      case class Uniform(c: String) extends Pattern
      case class Checker(c1: String, c2: String, size: Double) extends Pattern
      case class GradientX(c1: String, c2: String) extends Pattern
    }

    sealed trait Shape
    object Shape {
      case class Plane(
                        material: Material,
                        rotateX: Option[Double],
                        rotateY: Option[Double],
                        rotateZ: Option[Double],
                        translateX: Option[Double],
                        translateY: Option[Double],
                        translateZ: Option[Double],
                      ) extends Shape

      case class Sphere(
                         material: Material,
                         centerX: Double,
                         centerY: Double,
                         centerZ: Double,
                         radius: Double
                       ) extends Shape
    }

    case class PointLight(ptX: Double, ptY: Double, ptZ: Double, color: String)
    case class Camera(
                       fromX: Double,
                       fromY: Double,
                       fromZ: Double,
                       toX: Double,
                       toY: Double,
                       toZ: Double,
                       upX: Double,
                       upY: Double,
                       upZ: Double,
                       hRes: Int,
                       vRes: Int
                     )
    case class Scene(shapes: List[Shape], pointLight: PointLight, camera: Camera)

    @newtype case class DrawingId(value: String)

    sealed trait DrawingStatus
    object DrawingStatus {
      case object InProgress extends DrawingStatus
      case object Done extends DrawingStatus
    }

    case class DrawResponse(drawingId: DrawingId, status: DrawingStatus)
  }


}