# Ray tracing with environmental effects and ZIO

For an introduction to environmental effects, please refer to ZIO documentation, especially the [overview](https://zio.dev/docs/overview/overview_index) and the [module pattern](https://zio.dev/docs/howto/howto_use_module_pattern) sections.

# Building a ray tracer
A ray tracer simulates the behavior of rays of light as they hit the objects in a world, ending up hitting a camera sensors. There are a number of resources on the web to build ray tracers, but I followed [The Ray Tracer Challenge](https://pragprog.com/book/jbtracer/the-ray-tracer-challenge).

Since most of the rays acting upon a scene don't hit the camera, the most efficient process is to see how rays _outgoing_ from a camera hit the objects in the world, and then see how those rays actually originated, taking into account multiple effects such as shadows, light reflection and diffusion, and world reflection on reflective surfaces.

All rays and objects are described in terms of euclidean space, the space of points and vectors, and transformations such as translate, rotate, scale. 

We will implement a simple ray tracer using ZIO environmental effects, to demonstrate how we can build a true onion architecture application, and test it completely relying solely on functional effects.

## 1. Create a camera
A camera in a 3d space is completely described by its resolution (`hRes`, `vRes`), the point where it is located (`viewFrom`), the point it is looking at (`viewTo`) and the direction of the vector "looking up" the camera, to distinguish how the camera is rotated. Moreover, the aperture angle of the camera determines uniquely the distance between the "eye" and the camera itself (see Fig 1)

![Definition of a camera](images/camera.png)

In terms of code we can create a camera through a method with this signature (`UIO` is an effect that can't fail and doesn't need any environment to be provided in order to run):

```scala
object Camera {
  def make(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): UIO[Camera] = ???
}
```

This will create a(n `UIO` of a) `case class Camera( hRes: Int, vRes: Int, fieldOfViewRad: Double, tf: AT)`. While the first 3 parameters are obvious, what's the `tf` for? Actually, it is handy to start from a camera in a standard position (looking at the negative z axis, being located in the origin, looking up on the y axis), which we will call _canonical camera_. Any camera in the world (positioned in any arbitrary position, looking to an any point, oriented in any way) can be defined in terms of a transformation operated on the canonical camera. We take the _inverse_ of that transformation, and we carry it together with the camera. E.g. if I translate the camera to the point `Pt(0, 0, -5)` the transformation that must be carried along with our camera will be the transformation that must be applied to the world, in order to produce the same effect if the camera was the canonical one. That means if the real camera is positioned at `Pt(0, 0, -5)` it is the same thing to have a canonical camera, with the whole world translated by pf +5 on the `z` axis, i.e. a `translate(0, 0, 5)` transformation. This transformation will be carried along with the camera object.

![Show the effect of translating a camera](images/camera-translation.png)

So we will start with the definition of a module that gives us the capability of applying, and defining, these transformations, that are called _affine transformations_ and are completely specified by a matrix 4 x 4.

### 1.1. Define an Affine Transformations module
Affine transformations can be applied both to points and vectors. With the convention that a point `Pt(x, y, z)` is represented by a column vector $`[x, y, z, 1]^T`$, and a vector `Vec(x, y, z)` is represented by a vector $`[x, y, z, 0]`$, given an affine transformation matrix, applying that transformation to the vector or point means simply multiplying the transformation matrix by the vector representing the `Vec` or the `Pt`. To make things more efficient, given that often in our computations for every transformation we need the opposite one (think about changing perspective and observer relativity), we will compute the inverse at the moment of creation of an affine transformation.

```scala
abstract sealed case class AT private (direct: M, inverse: M) {
  def inverted: AT = AT(inverse, direct)
}
```

And then we need to define a module (i.e. a capability) that allows us to operate on these transformations. We will follow the ZIO convention in the module definition.

Let's start with the definition of the service for the module responsible of managing affine transformations, which resides in the module's companion object. Core methods define the capability of applying the affine transformation to vectors, points, and to compose 2 transformations. For the whole list of methods refer to the source code in the project.

```scala
object ATModule {
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, AlgebraicError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def compose(first: AT, second: AT): ZIO[R, AlgebraicError, AT]
```

Then we define a trait `ATModule` with only one value named `aTModule` of type `Service[Any]` (the convention is to give to this `val` the same name of the module, with the first letter lowercased)

```scala
trait ATModule {
  val aTModule: ATModule.Service[Any]
}
```

In the companion object we define also an _accessor_ object that by convention will be named `>` to access the capabilities  of our `ATModule` from arbitrary locations

```scala
  object > extends Service[ATModule] {
    def applyTf(tf: AT, vec: Vec): ZIO[ATModule, AlgebraicError, Vec] =
      ZIO.accessM(_.aTModule.applyTf(tf, vec))
    def applyTf(tf: AT, pt: Pt): ZIO[ATModule, AlgebraicError, Pt] =
      ZIO.accessM(_.aTModule.applyTf(tf, pt))
    def compose(first: AT, second: AT): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.compose(first, second))
```

This is a very mechanical and repetitive operation (indeed automated by an annotation available in `zio-macros`) but it unlocks a very powerful thing. It allows us to access methods that express the dependency on the `ATModule`, from any place in our code, for example 

```scala
val rotatedPt: ZIO[ATModule, AlgebraicError, Pt] = for {
  rotateX <- ATModule.>.rotateX(math.Pi / 2) 
  res     <- ATModule.>.applyTf(rotateX, Pt(1, 1, 1))
} yield  res
```


### 1.2 Implement a Live `ATModule`

Now that we have specified the abstract part of our module, we need to provide at least one implementation of it. In the project, I defined a module `MatrixModule` that can handle operations on matrices of arbitrary dimensions, therefore it will be able to perform the simple multiplications of our 4 x 4 matrices (affine transformations) by vectors 4 x 1 (points and vectors). Our live implementation of `ATModule` has a dependency on a `MatrixModule`. There are many options to express this dependency, one is relying on arrows that can be expressed as `ZIO[MatrixModule, Nothing, ATModule]`, but the simplest way is just declaring a field of type `MatrixModule.Service[Any]` that adheres to the naming convention (`matrixModule` is the name of the `val` for the service of `MatrixModule`)

```scala
trait Live extends ATModule {
    val matrixModule: MatrixModule.Service[Any]

    val aTModule: ATModule.Service[Any] = new ATModule.Service[Any] {
      import vectorizable.comp

      override def applyTf(tf: AT, vec: Vec): ZIO[Any, AlgebraicError, Vec] =
        for {
          col    <- PointVec.toCol(vec)
          colRes <- matrixModule.mul(tf, col)
          res    <- PointVec.colToVec(colRes)
        } yield res
```

The consequence of this is that given a computation that requires an `ATModule`, once provided with a `Live` implementation through `provide`, the compiler  will signal clearly which dependency is missing, and our naming convention makes it straightforward for us to provide the missing one

```scala
rotatedPt.provide(new ATModule.Live{})
// Compiler error:
// object creation impossible, since value matrixModule in trait Live of type io.tuliplogic.raytracer.geometry.matrix.MatrixModule.Service[Any] is not defined
// [error]   rotatedPt.provide(new ATModule.Live{})
```

This error can be solved just by providing an implementation of the `MatrixModule` as suggested by the compiler, so we provide a Breeze-backed matrix operations module implementation, mixed in with our `ATModule`, and everything compiles.

```scala
rotatedPt.provide(new ATModule.Live with MatrixModule.BreezeMatrixModule{}) 
// Compiles!
```

### 1.3. Make a Camera
With the capability of applying affine transformations, it is relatively easy to create a `Camera`, once we compute the transformation to be embedded in the camera (and for that we just need `ATModule` capabilities)

```scala
 def make(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): ZIO[ATModule, AlgebraicError, Camera] = for {
    cameraTf <- viewTransform(viewFrom, viewTo, upDirection)
  } yield new Camera(hRes, vRes, visualAngleRad, cameraTf)

  def viewTransform(from: Pt, to: Pt, up: Vec): ZIO[ATModule, AlgebraicError, AT] = ??? // one translation plus one orientation combined
```

# 2. Create a World
A world is just a set of shapes, and to produce nice images, spheres and planes are sufficient so these will be the only shapes we support. Like we did for the camera, we start defining 2 canonical shapes:
- a `Sphere.canonical` is defined as a sphere centered in the origin, of radius 1. As a set of points, $`\{(x, y, z) : x^2 + y^2 + z^2 = 1\}`$
- a `Plane.canonical` is the horizontal plane in our standard reference system, $`\{(x, y, z) : y = 0\}`$

Any other shape is represented as transformation applied to a canonical shape, so e.g. a Sphere centered in `Pt(0, 0, 5)` is represented as a `Sphere` equipped with a transformation to be applied to all the points of a canonical sphere, to produce the same sphere, it is clear that the transformation must simply be an `ATModule.translate(0, 0, 5)`. Same approach to describe generic planes.  

We provide also some utility methods to create uniform spheres, uniform planes, checker pattern planes etc. To build a world we also need a `PointLight` which can be simply defined as 

At the end of the day, all we need to build a world is (let's forget about colors of our spheres and planes for now)

```scala 
sealed trait SceneObject
case class Sphere(transformation: AT) extends SceneObject
case class Plane(transformation: AT) extends SceneObject

case class PointLight(position: Pt, intensity: Color)

case class World(pointLight: PointLight, objects: List[SceneObject])
```

# 3. Render a `World`






