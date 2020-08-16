autoscale: true
slidenumbers: false
build-lists: true
list: alignment(left)
theme: Fira, 3

^Plan for the talk: 
1. ZIO[R, E, A] lends to the module pattern
2. recipe for the module, show metrics example
3. Build a web application:
  - user management
  - image management
4. Show how to define `UserRepo` and then `UserService`
5. Routes with Tapir. Show how modul
3. Ray tracing problem
4. module for Transformations, relying on Matrix module (just signatures)
5. Module for camera, show rastering and testing
6. Show modules adding progressively
7. Show plugging in/out
8. Show http4s?


# `ZLayer`

## Let's build a full application
##### Pierangelo Cecchetto

<br/>

**LambdaConf 2020**
**18 August 2020**

<br/>
<br/>

![right fit](img/refractive-spheres.png) 

---
# ZIO-101

```scala
ZIO[-R, +E, +A]

       ⬇

R => IO[Either[E, A]]

       ⬇

R => Either[E, A]
```

---
# ZIO-101: Programs

[.list: alignment(left)]
[.build-lists: true]

- ZIO programs are values
- Concurrency based on fibers (green threads)

[.code-highlight: none]
[.code-highlight: 1-4]
[.code-highlight: 1-6]
[.code-highlight: 1-8]
[.code-highlight: 1-10]
```scala
  val prg: ZIO[Console with Random, Nothing, Long] = for {
    n <- random.nextLong                    // ZIO[Random, Nothing, Long]
    _ <- console.putStrLn(s"Extracted $n ") // ZIO[Console, Nothing, Unit]
  } yield n
 
  val allNrs: ZIO[Console with Random, Nothing, List[Long]] = ZIO.collectAll(List.fill(100)(prg))
  
  val allNrsPar: ZIO[Console with Random, Nothing, List[Long]] = ZIO.collectAllPar(List.fill(100)(prg))
  
  val allNrsParN: ZIO[Console with Random, Nothing, List[Long]] = ZIO.collectAllParN(10)(List.fill(100)(prg))
```

---
# ZIO-101: `R` means _requirement_

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-3]
[.code-highlight: 1-5]

```scala
val prg: ZIO[Console with Random, Nothing, Long] = ???

val autonomous: ZIO[Any, Nothing, Long] = ???

val getUserFromDb: ZIO[DBConnection, Nothing, User] = ???
```

---
# ZIO-101: Requirements elimination

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-4]
[.code-highlight: 1-6]

```scala
val getUserFromDb: ZIO[DBConnection, Nothing, User] = ???

val provided: ZIO[Any, Nothing, User] = 
  getUserFromDb.provide(DBConnection(...))

val user: User = Runtime.default.unsafeRun(provided)
```

---
# ZIO-101: Useful Aliases

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-3]
[.code-highlight: 1-4]
[.code-highlight: 1-5]

```scala
type IO[+E, +A]   = ZIO[Any, E, A]        
type Task[+A]     = ZIO[Any, Throwable, A]
type RIO[-R, +A]  = ZIO[R, Throwable, A]  
type UIO[+A]      = ZIO[Any, Nothing, A]  
type URIO[-R, +A] = ZIO[R, Nothing, A]    
```

---
# ZIO-101: Modules
Example: a module to collect metrics

[.code-highlight: 2-5] 
[.code-highlight: 1-5] 
[.code-highlight: all] 
```scala
type Metrics = Has[Metrics.Service]
object Metrics {
  trait Service {
    def inc(label: String): IO[Nothing, Unit]
  }

  //accessor method
  def inc(label: String): ZIO[Metrics, Nothing, Unit] =
      ZIO.accessM(_.get.inc(label))
  }
}
```

---
# ZIO-101: Modules
Example: a module for logging

```scala
type Log = Has[Log.Service]
object Log {
  trait Service {
    def info(s: String): IO[Nothing, Unit]
    def error(s: String): IO[Nothing, Unit]
  }

  //accessor methods...
}
```
---
# ZIO-101: Modules
Write a program using existing modules, i.e. _program to an interface_

```scala
val prg: ZIO[Metrics with Log, Nothing, Unit] = 
  for {
    _ <- Log.info("Hello")
    _ <- Metrics.inc("salutation")
    _ <- Log.info("Rotterdam")
    _ <- Metrics.inc("subject")
  } yield ()
```

---

# ZIO-101: The `Has` data type

`Has[A]` is a dependency on a service of type `A`

[.code-highlight: none] 
[.code-highlight: all] 
```scala
val hasLog: Has[Log.Service]         // type Log     = Has[Log.Service]
val hasMetrics: Has[Metrics.Service] // type Metrics = Has[Metrics.Service]
val mix: Log with Metrics = hasLog ++ hasMetrics

//access each service
mix.get[Log.Service].info("Starting the application")
```

---

# ZIO-101: The `Has` data type

^From the compiler POV, the types we are building are types mixins, so they play well with contravariance on the R
But under the hood we have the capability to re-access the single components

```scala
val mix: Log with Metrics = hasLog ++ hasMetrics

mix.get[Log.Service].info("Starting the application")
```

- To the compiler this looks like trait mixin 
- Plays well with `ZIO[-R, _, _]`
- Is backed by a heterogeneus map `ServiceType -> Service`
- Can replace/update services

---

# ZIO-101: `ZLayer`

```scala
ZLayer[-RIn, +E, +ROut]
```

- A recipe to build an `ROut`
- Backed by `ZManaged`: safe acquire/release
- `type Layer[+E, +ROut] = ZLayer[Any, E, ROut]`
- `type ULayer[+ROut]    = ZLayer[Any, Nothing, ROut]`


---

# ZIO-101: `ZLayer`

Construct from value

```scala
val layer: ULayer[UserRepo] = 
  ZLayer.succeed(new UserRepo.Service)
```
---
# ZIO-101: `ZLayer`

Construct from function

```scala
val layer: URLayer[Connection, UserRepo] = 
  ZLayer.fromFunction { c: Connection =>
    new UserRepo.Service
  }
```

---
# ZIO-101: `ZLayer`

Construct from effect

```scala
import java.sql.Connection

val e: ZIO[Connection, Error, UserRepo.Service]

val layer: ZLayer[Connection, Error, UserRepo] = 
  ZLayer.fromEffect(e)
```

---
# ZIO-101: `ZLayer`

Construct from resources

```scala
import java.sql.Connection

val connectionLayer: Layer[Nothing, Has[Connection]] = 
  ZLayer.fromAcquireRelease(makeConnection) { c =>
     UIO(c.close())
  }
```

---
# ZIO-101: `ZLayer`

Construct from other services

```scala
val usersLayer: URLayer[UserRepo with UserValidation, BusinessLogic] = 
  
  ZLayer.fromServices[UserRepo.Service, UserValidation.Service] { 
    (repoSvc, validSvc) =>
      new BusinessLogic.Service {
        // use repoSvc and validSvc
      }
    }
```

---
# ZIO-101: `ZLayer`

Compose horizontally
(_all inputs for all outputs_)

```scala
val l1: ZLayer[Connection, Nothing, UserRepo]
val l2: ZLayer[Config, Nothing, AuthPolicy]

val hor: ZLayer[Connection with Config, Nothing, UserRepo with AuthPolicy] =
  l1 ++ l2
```


---
# ZIO-101: `ZLayer`

Compose vertically
(_output of first for input of second_)

```scala
val l1: ZLayer[Config, Nothing, Connection]
val l2: ZLayer[Connection, Nothing, UserRepo]

val ver: ZLayer[Config, Nothing, UserRepo] =
  l1 >>> l2
```

---
# ZIO-101: `ZLayer`
`ZIO` uses ZLayer to provide the basic modules, all bundled in `ZEnv`

---

# Digression

- What is FP?
  - Referential Transparency :+1:
  - Immutability :+1:
  - Modularity and composability! :rocket:
- `ZLayer` is a tool to compose dependency trees, with arbitrary complexity

---

# Build a simple application

Given: A module that computes a png from scene description[^1] 

```scala
case class SceneBundle(world: World, viewFrom: Pt, viewTo: Pt) // a bit simplified

object PngRenderer {

  trait Service {
    def draw(scene: SceneBundle): UIO[Chunk[Byte]]
  }

  def draw(scene: SceneBundle): URIO[PngRenderer, Chunk[Byte]] =
    ZIO.accessM(_.get.draw(scene))

  val live: URLayer[CanvasSerializer with RasteringModule with ATModule, PngRenderer] = ???
}
```

[^1]: left over from a previous PoC about ZIO modularity

---

# A simple application

- Wrap in http layer
- Minimal user management

---

#### User Management / UserRepo

```scala
object UsersRepo {

  trait Service {
    def createUser(user: User): IO[DBError, Unit]
    def getUser(userId: UserId): IO[DBError, Option[User]]
    def getUserByEmail(email: Email): IO[DBError, Option[User]]
    def updatePassword(userId: UserId, newPassword: PasswordHash): IO[DBError, Unit]
    def updateAccessToken(
      userId: UserId, newAccessToken: AccessToken, expiresAt: ZonedDateTime
    ): IO[DBError, Unit]
  }

  /* and accessor methods */
```

---

#### User Management / UserRepo

```scala
val doobieLive: URLayer[DB.Transactor, UsersRepo] =
  ZLayer.fromService[HikariTransactor[Task], UsersRepo.Service] { 
    transactor =>
    new Service {

      def getUser(userId: UserId): IO[DBError, Option[User]] = {
        Queries.getUser(userId)
          .option.transact(transactor)
          .mapError(e => 
            DBError(s"Error fetching user with id = $userId", Some(e))
          )
      }
    }
  }
```

```scala
object Queries {
  def getUser(userId: UserId): Query0[User] =
    sql"""select * from users
          |  where id = ${userId.value}
          """.stripMargin.query[User]
}
```

![right fit](img/users-table.png)


---

#### User Management / Service

```scala
object Users {

  trait Service {
    def createUser(email: Email): IO[APIError, UserCreated]
    def updatePassword(email: Email, newPassword: ClearPassword): IO[APIError, PasswordUpdated]
    def login(userEmail: Email, givenPassword: ClearPassword): IO[APIError, LoginSuccess]
  }
```

---

#### User Management / Service

[.code-highlight: 1-7] 
[.code-highlight: 1-10] 
[.code-highlight: 1-7, 11-13] 
[.code-highlight: 1-7, 17] 
[.code-highlight: 1-7, 18-23] 
[.code-highlight: 1-7, 24] 
```scala
val live: URLayer[UsersRepo with Logging with Clock, Has[Service]] =
  ZLayer.fromServices[UsersRepo.Service, Logger[String], Clock.Service, Service] { (usersRepo, logger, clock) =>

  new Service {

    def login(userEmail: Email, clearPassword: ClearPassword): IO[APIError, LoginSuccess] =
        for {
          maybeUser <- usersRepo.getUserByEmail(userEmail).catchAll(e =>
            logger.throwable("DB error fetching user by email", e) *> ZIO.fail(APIError("Couldn't fetch user"))
          )
          user      <- maybeUser.fold[IO[APIError, User]](
            ZIO.fail(APIError("User not found"))
            )(u => ZIO.succeed(u))
          pwdHash   <- user.password.fold[IO[APIError, PasswordHash]](
              ZIO.fail(APIError("Password not set for user, cannot authenticate"))
            )(ZIO.succeed(_))
          newToken  <- createToken(clearPassword, pwdHash)
          now       <- clock.instant
          _ <- usersRepo.updateAccessToken(user.id, newToken, now.atZone(ZoneId.of("UTC")))
                .catchAll { dbErr => 
                  logger.throwable("DB Error updating access token", dbErr)
                    .as(APIError("Could not update access token, you must login again"))
                }
        } yield LoginSuccess(user.id, newToken)
```

---
^Let's go now to the Http layer definition. For this, my tool of choice is TAPIR.
First of, it adheres to the functional design principles of composability and modularity. Endpoints are values

#### Http Layer

Tapir: **endpoints as values**

```scala
val login: Endpoint[Login, APIError, LoginSuccess, Nothing] =
  endpoint.post.in("login").in(jsonBody[Login]).out(jsonBody[LoginSuccess]).errorOut(jsonBody[APIError])
    .description("Login to obtain an access token")
```

---
^The first thing we can do with this, is have documentation for free

#### Http Layer

Tapir: OpenAPI  **documentation for free**

```scala
val openApiDocs: OpenAPI = Seq(
  ...
  endpoints.login,
  ...
).toOpenAPI("Ray Tracing as a Service", "1.0")
  .servers(List(Server("localhost:8090").description("local server")))

val docsRoutes: HttpRoutes[Task] = new SwaggerHttp4s(openApiDocs.toYaml).routes[Task]
```

![right 60%](img/swagger.png)

---

^Sencondly, Tapir has a fabulous integration with ZIO
#### Http Layer

Tapir: Integration with ZIO

[.code-highlight: 1-5] 
[.code-highlight: 1-8] 
[.code-highlight: 1-9] 
[.code-highlight: 1-14] 
```scala
// bind endpoint with module
val loginWithLogic: ZServerEndpoint[Users, Login, APIError, LoginSuccess] =
  endpoints.login.zServerLogic(login =>
    Users.login(login.email, login.password)
  )

//make HttpRoutes for http4s
val loginRoute:    URIO[Users, HttpRoutes[Task]]  = loginWithLogic.toRoutesR
val getSceneRoute: URIO[Scenes, HttpRoutes[Task]] = getSceneWithLogic.toRoutesR

val serve: RIO[Users with Scenes with Logging, Unit] = for {
  allRoutes <- ZIO.mergeAll(List(loginRoute, getSceneRoute))(docsRoutes)(_ <+> _)
  _         <- serveRoutes(allRoutes)
} yield ()
```

---

^Only in the main we provide layer
#### Putting things together

```scala
val program: ZIO[Users with Logging with Transactor with Scenes, BootstrapError, Unit] =
  for {
    _ <- log.info("Running Flyway migration...")
    _ <- DB.runFlyWay
    _ <- log.info("Flyway migration performed!")
    _ <- serve.mapError(e => BootstrapError("Error starting http server", Some(e)))
  } yield ()

override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
  program.provideCustomLayer(???)


//we are looking for a Layer[?, Nothing, Users with Logging with Transactor with Scenes]
```

---
#### Putting things together

As usual, follow the types!

```scala
val program: ZIO[Users with Logging with Transactor with Scenes, BootstrapError, Unit] 

//Look for the layer producing the module we need
val live: URLayer[UsersRepo with Logging with Clock, Users] = ???



```





---

[.code-highlight: 1-7] 
[.code-highlight: 1-14] 
[.code-highlight: 1-20] 
```scala
type UserRepo = Has[UserRepo.Service]

object UserRepo {
  trait Service {
    def getUser(userId: UserId): IO[DBError, Option[User]]
    def createUser(user: User): IO[DBError, Unit]
  }

  val inMemory: Layer[Nothing, UserRepo] = ZLayer.succeed(
    new Service {
      /* impl */
    }
  )

  val db: ZLayer[Connection, Nothing, UserRepo] = ZLayer.fromService { conn: Connection =>
    new Service {
      /* impl uses conn */
    }   
  }
}
```

---
# ZIO-101: `ZLayer`

Provide layers to programs

[.code-highlight: 1-2] 
[.code-highlight: 1-8] 
[.code-highlight: 1-12] 
[.code-highlight: 1-16] 
```scala
  import zio.console.Console 
  val checkUser: ZIO[UserRepo with AuthPolicy with Console, Nothing, Boolean]
  
  val liveLayer = UserRepo.inMemory ++ AuthPolicy.basicPolicy ++ Console.live

  val full: ZIO[Any, Nothing, Boolean] = checkUser.provideLayer(
    liveLayer
  )

  val partial: ZIO[Console, Nothing, Boolean] = checkUser.provideSomeLayer(
    UserRepo.inMemory ++ AuthPolicy.basicPolicy
  )

  val updated: ZIO[Any, Nothing, Boolean] = checkUser.provideLayer(
    liveLayer ++ UserRepo.postgres
  )
```

---

^In ray tracing we have 3 components: 
- The world (of spheres), and ambient light
- A Light source
- A Camera
- Reflected Rays
- Discarded rays
- Canvas 

![left fit](img/raytracing-description-1.png) 

# Ray tracing
[.list: alignment(left)]

- World (spheres), light source, camera
- Incident rays    
- Reflected rays

---
^To build an image we need a canvas, a rectangular surface divided in pixels where the rays coming from the world will hit and produce the color they carry in the ray

![left fit](img/raytracing-description-2.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- World (spheres), light source, camera
- Incident rays    
- Reflected rays
- Discarded rays
- Canvas

---

![left fit](img/raytracing-description-3.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- World (spheres), light source, camera
- Incident rays    
- Reflected rays
- Discarded rays
- Canvas
- Colored pixels

---

![left fit](img/raytracing-description-4.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- World (spheres), light source, camera
- Incident rays    
- Reflected rays
- Discarded rays
- Canvas
- Colored pixels

---
^At this point we have 2 options, one is computing all the rays for all the objects, and then consider only those that hit the canvas
Another option is work on the reverse problem, i.e. have rays going out of the canvas, hitting the objects in the world and determine how they behave considering all the agents

![left fit](img/raytracing-description-4.png) 

# Ray tracing

Options:

[.list: alignment(left)]
1. Compute all the rays (and discard most of them)
1. Compute only the rays outgoing from the camera through the canvas, and determine how they behave on the surfaces

<!--
^Let's start building our model. A ray is an infinite line with a starting point. So let's start with the components we need to represent it, i.e. points and vectors

![left fit](img/rt-ray-def-tex.png) 

# Ray

A ray is defined by the point it starts from, and its direction

$$
P(t) = P_0 + t \vec{D},   t > 0 
$$

---

# Foundations
^ The foundations of this all are points and vectors, which are both represented by 3 numbers, but the difference is that vectors can be added with each othr, but points cannot. What you can do with points is add a vector to a point to get another point. And what you are building this way is an affine space

![left fit](img/pt-vec-combined.png) 

[.list: alignment(left)]
- Points and Vectors
- Transformations (rotate, scale, translate)

---
^ It's pretty easy to encode this in simple scala... and to verify that they satisfy the properties we just mentioned 

![left fit](img/pt-vec-combined.png) 


### Points and Vectors

[.code-highlight: 1-6]
[.code-highlight: 1-14]
```scala
case class Vec(x: Double, y: Double, z: Double) {
  def +(other: Vec): Vec   = 
    Vec(x + other.x, y + other.y, z + other.z)
  def unary_- : Vec = 
    Vec(-x, -y, -z)
}

case class Pt(x: Double, y: Double, z: Double) {
  def -(otherPt: Pt): Vec =
    Vec(x - otherPt.x, y - otherPt.y, z - otherPt.z)
  def +(vec: Vec)         = 
    Pt(x + vec.x, y + vec.y, z + vec.z)
}
```

---
^The first thing we do when we build our representation is to establish properties, so we write some PBT to check that 
* `(Vec, +)` form a group, 
* `(Vec, Pt, +, -)` form an affine space
* `approx` is due to double finite precision

![left fit](img/pt-vec-combined.png) 

### Points and Vectors

`zio-test` for PBT

[.code-highlight: 1-7]
[.code-highlight: 1-14]
```scala
testM("vectors form a group")(
  check(vecGen, vecGen, vecGen) { (v1, v2, v3) =>
    assertApprox  (v1 + (v2 + v3), (v1 + v2) + v3) &&
    assertApprox (v1 + v2 , v2 + v1) &&
    assertApprox (v1 + Vec.zero , Vec.zero + v1)
  }
),


testM("vectors and points form an affine space") (
  check(ptGen, ptGen) { (p1, p2) =>
    assertApprox (p2, p1 + (p2 - p1))
  }
)
```
-->

---
^once we define points and vectors, the definition of a ray is immediate:

![left fit](img/rt-ray-def-tex.png) 

### Ray

$$
P(t) = P_0 + t \vec{D},   t > 0 
$$

```scala
case class Ray(origin: Pt, direction: Vec) {
  def positionAt(t: Double): Pt =
    origin + (direction * t)
}
```

---
^ Provided 
* we have an AT type for our affine transformation 
* let's define a ZIO module to operate on  transformations, so we start defining a Service to apply them to point, vectors, and chain/compose them
* Define a trait that has a val with the same name of the module, pointing to the service we just created
* Define an object extending the Service, parameterized on the module itself. 
* The accessor creation is pretty repetitive and mechanical, so an annotation can do the boring stuff for us (the same way simulacrum does for typeclasses)

### Transformations Module

```scala
trait AT

type ATModule = Has[ATModule.Service]
object ATModule {
  /* Service */
  trait Service {
    def applyTf(tf: AT, vec: Vec): ZIO[ATError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[ATError, Pt]
    def compose(first: AT, second: AT): ZIO[ATError, AT]
  }

  def applyTf(tf: AT, vec: Vec): ZIO[ATModule, ATError, Vec] =
    ZIO.accessM(_.aTModule.applyTf(tf, vec))
  def applyTf(tf: AT, pt: Pt): ZIO[ATModule, ATError, Pt] =
    ZIO.accessM(_.aTModule.applyTf(tf, pt))
  def compose(first: AT, second: AT): ZIO[ATModule, ATError, AT] =
    ZIO.accessM(_.aTModule.compose(first, second))
}
```

---
^ This accessor object allows us to summon our module wherever necessary, and build something like this, where I'm summoning capabilities from different modules, not only, but the compiler is able to infer and mix these capabilities for us
### Transformations Module

[.code-highlight: 1-26]
```scala
val rotatedPt = 
  for {
    rotateX <- ATModule.rotateX(math.Pi / 2)
    _       <- Log.info("rotated of π/2")
    res     <- ATModule.applyTf(rotateX, Pt(1, 1, 1))
  } yield  res
```
---

^ Show type inference
### Transformations Module

[.code-highlight: 1-26]
```scala
val rotatedPt: ZIO[ATModule with Log, ATError, Pt] =
  for {
    rotateX <- ATModule.rotateX(math.Pi / 2)
    _       <- Log.info("rotated of π/2")
    res     <- ATModule.applyTf(rotateX, Pt(1, 1, 1))
  } yield  res
```

---

^ Now we want to provide an implementation of this module. With an easy convention to translate vectors and points into 4-tuples, all we need is matrix multiplication  
### Transformations Module - Live

```scala
val rotated: ZIO[ATModule, ATError, Vec] = 
  for {
    rotateX <- ATModule.rotateX(math.Pi/2)
    res     <- ATModule.applyTf(rotateX, Pt(1, 1, 1))
  } yield res
```

[.list: alignment(left)]
- `Vec(x, y, z)`  $$ \Rightarrow [x, y, z, 0]^T$$ 

- `Pt(x, y, z)⠀`  $$ \Rightarrow [x, y, z, 1]^T$$ 

- ⠀
$$
\mathtt{rotated} = \begin{pmatrix}
\cos \pi/2 & -\sin \pi/2 & 0 & 0\\
\sin \pi/2 & \cos \pi/2 & 0 & 0\\
0 & 0 & 1 & 0 \\
0 & 0 & 0 & 1 \\
\end{pmatrix}
\begin{pmatrix}
x\\
y\\
z \\
0\\
\end{pmatrix}
$$

---

### Transformations Module - Live

$$
\mathtt{rotated} = \begin{pmatrix}
\cos \pi/2 & -\sin \pi/2 & 0 & 0\\
\sin \pi/2 & \cos \pi/2 & 0 & 0\\
0 & 0 & 1 & 0 \\
0 & 0 & 0 & 1 \\
\end{pmatrix}
\begin{pmatrix}
x\\
y\\
z \\
0\\
\end{pmatrix}
$$


```scala
val live: ZLayer[MatrixModule, Nothing, ATModule] = 
  ZLayer.fromService { matrixSvc =>
    new Service {
      def applyTf(tf: AT, vec: Vec) = 
        matrixSvc.mul(tf.direct, v)
      
      /* ...  */   
    }
}
```
---
^So now we have a tool that allows us to perform the std transformations on our points and vectors, so we are ready to build a camera

### Layer 1: Transformations

![inline 80%](img/rotate-animated.gif)![inline 80%](img/translate-animated.gif)![inline 80%](img/scale-animated.gif)

<!--

---
^The first thing to consider is that everything is relative. If my standard position as observer is x=0, and I want to see how things look like from x = -3, what I can do is translate the world of +3 and keep on sitting at x = 0. We call this the canonical position
### Camera 

##### Everything is relative!

![inline](img/camera-translate.png) 

- Canonical camera: observe always from `x = 0` and translate the world by `+3`

---
^In the same spirit, let's define our canonical camera

![left fit](img/canonical-camera.png) 

### Camera - canonical

[.code-highlight: 1-4, 6]
[.code-highlight: 1-6]
```scala
case class Camera (
  hRes: Int,
  vRes: Int,
  fieldOfViewRad: Double,
  tf: AT // world transformation
)
```

-->

---

### Camera
^Following the same reasoning we did with our camera translated in x=-3, we can generalize and build a generic camera by composing 2 transformations, one coping for the rotations, and one for the translations that brought our camera there. We have just to use the inverse because we will apply them to the world

![left fit](img/rotated-camera.png) 

[.code-highlight: 1-8]
[.code-highlight: 1-20]
```scala
object Camera {
  def make(
    viewFrom: Pt, 
    viewTo: Pt, 
    upDirection: Vec, 
    visualAngleRad: Double, 
    hRes: Int, 
    vRes: Int): 
    ZIO[ATModule, AlgebraicError, Camera] =
    worldTransformation(viewFrom, viewTo, upDirection).map { 
      worldTf => Camera(hRes, vRes, visualAngleRad, worlfTf)
    }
```

---

^For shapes we follow the same approach we followed for camera, canonical + transformation

### World
- `Sphere.canonical` $$ \{(x, y, z) : x^2 + y^2 + z^2 = 1\} $$
- `Plane.canonical` $$\{(x, y, z) : y = 0\} $$

[.code-highlight: none]
[.code-highlight: 1-4]
[.code-highlight: 1-6]
[.code-highlight: 1-7]
```scala
sealed trait Shape {
  def transformation: AT
  def material: Material
}

case class Sphere(transformation: AT, material: Material) extends Shape
case class Plane(transformation: AT, material: Material) extends Shape
```
---
^Let's see how we can make a generic sphere, a generic plane, and put them in the world

### World

##### Make a world

[.code-highlight: none]
[.code-highlight: 1-7]
[.code-highlight: 1-11]
[.code-highlight: 1-13]
```scala
object Sphere {
  def make(center: Pt, radius: Double, mat: Material): ZIO[ATModule, ATError, Sphere] = for {
    scale     <- ATModule.scale(radius, radius, radius)
    translate <- ATModule.translate(center.x, center.y, center.z)
    composed  <- ATModule.compose(scale, translate)
  } yield Sphere(composed, mat)
}

object Plane {
  def make(...): ZIO[ATModule, ATError, Plane] = ???
}

case class World(pointLight: PointLight, objects: List[Shape])
```

- Everything requires `ATModule`

---
^Rendering a world means producing an image that reprsents how the world looks like from our camera. So from the highest level I want to be able to produce a stream of colored pixels representing my image. So we define the module, make it accessible, and provide a trivial implementation that produces white pixels no matter what

### World Rendering - Top Down
#### Rastering - Generate a stream of colored pixels

```scala
type RasteringModule = Has[Service]
object RasteringModule {
  trait Service {
    def raster(world: World, camera: Camera): 
      Stream[RayTracerError, ColoredPixel]
  }
}
```

---
^Now what do we need to provide a LIVE implementation of this? We need to be able to provide one ray for each pixel of the camera, and for each ray we need to compute the color. Let's introduce 2 modules with these responsibilities

![left fit](img/raytracing-description-4.png) 

### World Rendering - Top Down
#### Rastering - **Live**

- Camera module - Ray per pixel

[.code-highlight: none]
[.code-highlight: all]
```scala
type CameraModule = Has[Service]
object CameraModule {
  trait Service {
    def rayForPixel(
      camera: Camera, px: Int, py: Int
    ): UIO[Ray]
  }
}
```

- World module - Color per ray

[.code-highlight: none]
[.code-highlight: all]
```scala
type WorldModule = Has[Service]
object WorldModule {
  trait Service {
    def colorForRay(
      world: World, ray: Ray
    ): IO[RayTracerError, Color]
  }
}
```

---
^And with these 2 modules we can provide a live implementation of the rastering logic. We declare dependencies on the *services* of our modules, and then we access them in the implmentation

### World Rendering - Top Down
#### Rastering **Live** - Module Dependency

[.code-highlight: 1-5]
[.code-highlight: 1-6]
[.code-highlight: all]
```scala
val chunkRasteringModule: ZLayer[CameraModule with WorldModule, Nothing, RasteringModule] =
  ZLayer.fromServices[cameraModule.Service, worldModule.Service, rasteringModule.Service] {
    (cameraSvc, worldSvc) =>
      new Service {
        override def raster(world: World, camera: Camera): 
          Stream[Any, RayTracerError, ColoredPixel] = {
          val pixels: Stream[Nothing, (Int, Int)] = ???
          pixels.mapM{
            case (px, py) =>
              for {
                ray   <- cameraModule.rayForPixel(camera, px, py)
                color <- worldModule.colorForRay(world, ray)
              } yield data.ColoredPixel(Pixel(px, py), color)
          }
      }
    }
  }
```

---
^So the takeaway of this is...

### Layers

#### Takeaway
#### Implement and test every layer only in terms of the immediately underlying layer

---
^And now that we got warmed up with this, let's go on and implement all the logic through modules

![100%](img/modules-all-the-way-meme.jpg) 

---
^We can go on and fragment our logic in modules small enough to be tested and thought of in isolation

![left fit](img/modules-0.png) 

### Live **CameraModule**

```scala
object CameraModule {
  val live: ZLayer[ATModule, Nothing, CameraModule] = 
    ZLayer.fromService { atSvc => 
      /* ... */
    }
}
```

---
^And implement the world module

![left fit](img/modules-1.png) 
### Live **WorldModule**
#### **WorldTopologyModule**

[.code-highlight: none]
```scala
object WorldTopologyModule {
  trait Service {
    def intersections(world: World, ray: Ray): 
      UIO[List[Intersection]]
    
    def isShadowed(world: World, pt: Pt): 
      UIO[Boolean]
  }  
}
```
---
^World topology module solves this problem

![left fit](img/shadow.png) 
### Live **WorldModule**
#### **WorldTopologyModule**

[.code-highlight: all]
```scala
object WorldTopologyModule {
  trait Service {
    def intersections(world: World, ray: Ray): 
      UIO[List[Intersection]]
    
    def isShadowed(world: World, pt: Pt): 
      UIO[Boolean]
  }  
}
```

---
^And implement the world module

![left fit](img/modules-2.png) 
### Live **WorldModule**
#### **WorldHitCompsModule**

[.code-highlight: none]
```scala
case class HitComps(
  shape: Shape, hitPt: Pt, normalV: Vec, eyeV: Vec, 
  rayReflectV: Vec, n1: Double = 1, n2: Double = 1
)

object WorldHitCompsModule {
  trait Service {
    def hitComps(
      ray: Ray, hit: Intersection, 
      intersections: List[Intersection]
    ): IO[GenericError, HitComps]
  }
}

```
---
^World hit components module

![left fit](img/hit-components.png) 
### Live **WorldModule**
#### **WorldHitCompsModule**

```scala
case class HitComps(
  shape: Shape, hitPt: Pt, normalV: Vec, eyeV: Vec, 
  rayReflectV: Vec, n1: Double = 1, n2: Double = 1
)

object WorldHitCompsModule {
  trait Service {
    def hitComps(
      ray: Ray, hit: Intersection, 
      intersections: List[Intersection]
    ): IO[GenericError, HitComps]
  }
}
```
---

![left fit](img/modules-3.png) 
### Live **WorldModule**
#### **PhongReflectionModule**

[.code-highlight: none]
```scala
case class PhongComponents(
  ambient: Color, diffuse: Color, reflective: Color
) {
  def toColor: Color = ambient + diffuse + reflective
}

object PhongReflectionModule {
  trait Service {
    def lighting(
      pointLight: PointLight, hitComps: HitComps, 
      inShadow: Boolean
    ): UIO[PhongComponents]
  }
}
```
---
^World hit components module

![left fit](img/hit-components.png) 
### Live **WorldModule**
#### **PhongReflectionModule**

[.code-highlight: all]
```scala
case class PhongComponents(
  ambient: Color, diffuse: Color, reflective: Color
) {
  def toColor: Color = ambient + diffuse + reflective
}

object PhongReflectionModule {
  trait Service {
    def lighting(
      pointLight: PointLight, hitComps: HitComps, 
      inShadow: Boolean
    ): UIO[PhongComponents]
  }
}
```


---
^So far we didn't look at the color of objects, or their material properties, but that's what makes the colors of an object. So we define a data type that collects the material properties that make how it looks like
*Ambient models the presence of an "ambient" light uniformly distributed in the world
*Diffusion models the behavior of light hitting a matte material and assumes that the effect of light hitting a material at a give point is just depending on the projection of the ray of light on the normal vector to the surface at that point. This will affect any eye observing that point in LOS.
*Specularity/shininess model how the source of light is reflected by the material, i.e. how you are going to see that lamp reflected on the object itself.


![left fit](img/specular-shininess.png) 
### Reflect the ligtht source

Describe material properties

```scala
case class Material(
  color: Color, // the basic color
  ambient: Double,  // ∈ [0, 1] 
  diffuse: Double,  // ∈ [0, 1]
  specular: Double, // ∈ [0, 1]
  shininess: Double, // ∈ [10, 200]
)
```
---

![left fit](img/modules-5.png) 
### Live **PhongReflectionModule**
#### With **LightDiffusion** and **LightReflection** 


```scala
object PhongReflectionModule {
  trait Service { }

  val live: ZLayer[ATModule 
    with LightDiffusionModule 
    with LightReflectionModule, 
    Nothing, 
    PhongReflectionModule]
}
```

---
![left fit](img/modules-5.png) 
### Drawing program

```scala
def draw(sceneBundle: SceneBundle): 
  ZIO[CanvasSerializer 
    with RasteringModule 
    with ATModule,
    Nothing, 
    Array[Byte]]
```

---
![left fit](img/modules-5.png) 
### With Http4s

```scala
class DrawRoutes[R <: CanvasSerializer with RasteringModule with ATModule] {
  type F[A] = RIO[R, A]
  private val http4sDsl = new Http4sDsl[F] {}
  import http4sDsl._

  val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "draw" =>
      req.decode[Scene] { scene =>
        (for {
          bundle    <- Http2World.httpScene2World(scene)
          bytes     <- draw(bundle)
        } yield bytes).foldM {
          e => InternalServerError(s"something went wrong"),
          Ok(bytes, "image/png")
        }
      }
  }
}
```

---
![left fit](img/modules-5.png) 
# And in main
## **Provide the layers**

```scala
val world: ZLayer[ATModule, Nothing, WorldModule] = 
  (topologyM ++ hitCompsM ++ phongM) >>> worldModule.live  

val rastering: ZLayer[ATModule, Nothing, RasteringModule] = 
  (world ++ cameraModule.live) >>> rasteringModule.chunkRasteringModule

val full: Layer[Nothing, Rastering] = (layers.atM >>> rastering)

object Main extends zio.App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    httpProgram.provideLayer(full)
}
```
---

### **Swapping modules**

![left fit](img/refractive-without-refraction-blue.png) 

[.build-lists: false]
- Red: reflective = 0.9
- Green/white: reflective = 0.6
- Blue: reflective = 0.9, transparency: 1

```scala
val world: ZLayer[ATModule, Nothing, WorldModule] = 
  (topologyM ++ hitCompsM ++ phongM) >>> worldModule.opaque
```
--- 
[.autoscale: false]

### **Swapping modules**

![left fit](img/refractive-with-refraction-blue.png) 

[.build-lists: false]

- Red: reflective = 0.9
- Green/white: reflective = 0.6
- Blue: reflective = 0.9, transparency: 1


```scala
val world: ZLayer[ATModule, Nothing, WorldModule] = 
  (topologyM ++ hitCompsM ++ phongM) >>> worldModule.live
```
---

### Conclusion - **ZLayer**
[.text: alignment(left)]

- Dependency graph in the code 💪
- Type safety, no magic 🙌
- Compiler helps to satisfy requirements 🤗
- Try it out, and join ZIO Discord channel 😊

---

# **Thank you!**

![inline 10%](img/Twitter_Logo_WhiteOnBlue.png) @pierangelocecc

![inline 40%](img/GitHub-Mark-Light-120px-plus.png) https://github.com/pierangeloc/ray-tracer-zio
