# Unleash ZIO environment with `ZLayer`

`ZIO` is designed around 3 parameters, `R, E, A`. `R` represents the _requirement_ for the effect to run, meaning we need to fulfill
the requirement in order to make the effect _runnable_. We will dedicate this post to explore what we can do with `R`, as it plays a crucial role in `ZIO`.

## A Simple case
Let's build a simple program for a webshop, that must retrieve from DB the details of a product, together with the IDs of the related products. 
To access the DB we need a `DBConnection`, and each step in out program represents this through the environment type. We can then combine the two steps through the `zip` combinator,
producing a program that, in turn, depends on the `DBConnection`.

```scala
case class DBConnection(...)
val productDetails: ZIO[DBConnection, Nothing, Product]
val relatedProducts: ZIO[DBConnection, Nothing, List[ProductId]]

val getProductWithRelated: ZIO[DBConnection, Nothing, (Product, List[ProductId])] = 
    productDetails zip relatedProducts 
```

To run the program we must supply a `DBConnection` through `provide`, before feeding it to ZIO runtime.

```scala
val dbConnection: DBConnection = ???
val runnable: ZIO[Any, Nothing, (Product, List[ProductId])] = getProductWithRelated.provide(dbConnection)

val (product, related)  = runtime.unsafeRun(runnable)
```

Notice that the act of `provide`ing an effect with its environment eliminates the environment dependency in the resulting effect type, well represented by type `Any` of the resulting environment.

## Our first ZIO module
The module pattern is _the way_ ZIO manages dependencies between application components.
In its initial formulation it was only using trait mix-ins, as shown in [this talk](https://www.youtube.com/watch?v=IvL8mmB2RBM).

Here we will see the new formulation of the module pattern, that resolves most of the shortcomings of the previous version.

### What is a module?
A module is a group of functions that deals with only one concern. Keeping the scope of a module limited improves our ability to understand code, in that we can focus
 only about one concern at a time without juggling with too many concerns together in our head.
 
`ZIO` iself provides the basic capabilities through modules, see yourself how `ZEnv` is defined.



### The module recipe
Let's build a module for product data access, following these simple steps:

1. Define an object that gives the name to the module, this can be (not necessarily) a package object
1. Within the module object define a `trait Service` that defines the interface our module is exposing, in our case 2 methods to retrieve a product details, and one to retrieve the related products
1. Within the module object define a type alias like `type ModuleName = Has[Service]`
1. Within the module object define the different implementations of `ModuleName` through `ZLayer`

```scala
import zio.{Has, ZLayer}

object UserRepo {
  trait Service {
    def getProductDetails(productId: ProductId): IO[DBError, Product]
    def getRelatedProducts(productId: ProductId): IO[DBError, List[Product]]
  }
  
  type UserRepo = Has[Service]
  val testRepo: ZLayer.NoDeps[Nothing, UserRepo] = ???
  
}
```




The recipe to build a new module is:
1. define an object (usually small cased), e.g. `usersModule`, and in that object:
1. Define a generic `Service[R]` the usual way to define the algebra, or better the interface of the module
1. `type UsersModule = Has[Service[Any]]`
1. create accessor object the usual way
```scala
object > extends Service[UsersModule] {
  override def getUser(id: Long): ZIO[UsersModule, Nothing, String] =
  ZIO.accessM(_.get.getUser(id))
``` 
It differs from previous implementation in that you must add a .get on top of it, to access the service within an `Has` 


## APPENDIX: The _classic_ module formulation

Let's see how a service to retrieve product details was formulated in the classic way.

Here we define a module to cope with CRUD operations for the `User` domain object

```scala
trait UserRepo {
  val userRepo: UserRepo.Service
}

object UserRepo {
  trait Service {
    def getUser(userId: UserId): IO[DBError, Option[User]]
    def createUser(user: User): IO[DBError, Unit]
  }
  
  trait Live extends UserRepo {
    val dbConnection: Connection
    val userRepo: Service = new Service {
      private def runSql[A](sql: String): IO[DBError, A] = /* use dbConnection */

      def getUser(userId: UserId): IO[DBError, Option[User]] = runSql[Option[User]]("select * from users where id = $userId")
      def createUser(user: User): IO[DBError, Unit] = runSql[Unit](s"insert into users values (${user.id}, ${user.name}")
    }
  }

  //accessor methods
  def getUser(userId: UserId): ZIO[UserRepo, DBError, Option[User]] =
    ZIO.accessM(_.userRepo.getUser(userId))
  
  def createUser(user: User): ZIO[UserRepo, DBError, Unit] =
    ZIO.accessM(_.userRepo.createUser(user))

}
```

And a module to cope with logging

```scala
trait Logging {
  val logging: Logging.Service
}

object Logging {
  trait Service {
    def info(s: String): UIO[Unit]
    def error(s: String): UIO[Unit]
  }

  trait Live extends Logging {
    val logger: Logger
    val logging: Logging.Service = new Service {
      def info(s: String): UIO[Unit] = logger.info(s)
      def error(s: String): UIO[Unit] = logger.error(s)      
    }
  }
  
  //accesspr methods
  def info(s: String): ZIO[Logging, Nothing, Unit] = 
    ZIO.accessM(_.logging.info(s))

  def error(s: String): ZIO[Logging, Nothing, Unit] = 
    ZIO.accessM(_.logging.error(s))

}
```

Now we can combine operations provided by the different modules through the various combinators provided by ZIO, e.g.
```scala
val user = User(123, "Chet")
val makeUser: ZIO[Logging with UserRepo, Nothing, Unit] = for {
  _ <- Logging.info(s"inserting user") // ZIO[Logging, Nothing, Unit]
  _ <- UserRepo.createUser(user)       // ZIO[UserRepo, DBError, Unit]
  _ <- Logging.info(s"user inserted")  // ZIO[Logging, Nothing, Unit]
} yield ()
```

Note that the type of `makeUser` is fully inferred by the compiler, and it expresses the fact that to our program requires an environment of type `Logging with UserRepo`.

To run the program we must satisfy its requirements, and feed the corresponding value to ZIO runtime

```scala
val env: Logging with UserRepo = new Logging.Live with UserRepo.Live {
  val logger: Logger = Logger(LoggerFactory.getLogger("classic-module-pattern"))
  val dbConnection: Connection = ??? //this must be injected or passed somehow
}

val runnable: ZIO[Any, DBError, Unit] = makeUser.provide(env) // this effect has no requirements, it can be run 

defaultRuntime.unsafeRun(runnable)
```




#### Provide partial environments
Let's suppose we have our `makeUser: ZIO[Logging with UserRepo, Nothing, Unit] ` and we want to satisfy just part of the requirement, e.g. we want to keep the logging part but delay the 
provisioning of `UserRepo`. `ZIO[R, E, A]` has a method `provideSome[R0](f: R0 => R): ZIO[R0, E, A]` that builds the required environment starting from a part of it

```scala
val makeUserForRepo: ZIO[UserRepo, Nothing, Unit] = makeUser.provideSome[UserRepo] { env =>
  new Logging with UserRepo {
    val logging = Logging.Live.logging
    val userRepo = env.userRepo
  }
}
```

In this case we had a simple environment, but in case of environments coming from mixin many layers this process can be very tedious.

#### Vertical composition: a module depending on another module
In many cases we have modules depending on other modules, e.g. we could have a module `UserValidation` and a `User` module that depends on `UserRepo` and `Validation`. 
The way we can encode this is by forcing the depending module to be mixed in with the dependee modules. One way is to declare the services we need from the required modules as `val` of the module implementation (e.g. the `Live` implementation can have richer requirement than a test implementation)

```scala
trait UserValidation {
  val userValidation: UserValidation.Service 
}

object UserValidation {
  trait Service {
    def validate(user: User): IO[ValidationError, Unit]
  }

  trait Live { /* Live implementation, e.g. calling a webservice to identity document validity */}
}

trait UserService {
  val userService: UserService.Service
}
object UserService {
  trait Service {
    def registerUser(user: User): IO[ApplicationError, Unit]
    def getUser(userId: UserID): IO[ApplicationError, Option[User]]
  }
  
  trait Live extends UserService {
    val userRepo: UserRepo.Service
    val userValidation: UserValidation.Service

    val userService = new Service {
      def registerUser(user: User): IO[ApplicationError, Unit] = for {
        _ <- userValidation.validate(user)
        _ <- userRepo.createUser(user)
      } yield ()

      def getUser(userId: UserID): IO[ApplicationError, Option[User]] = userRepo.getUser(userId)
    }
  }

}
```

When we have a program that requires `UserService`, e.g. `prg: ZIO[UserService, Nothing, Unit]` we can fulfull the requirements starting from the required module, e.g. `prg.provide(new UserService)`, the compiler will guide us in fulfilling all the required modules by mixing in all the modules our module depends on.