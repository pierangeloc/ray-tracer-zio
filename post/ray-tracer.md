# Ray tracing with environmental effects and ZIO

For an introduction to environmental effects, please refer to ZIO documentation, especially the [overview](https://zio.dev/docs/overview/overview_index) and the [module pattern](https://zio.dev/docs/howto/howto_use_module_pattern) sections.

# Building a ray tracer
A ray tracer simulates the behavior of rays of light as they hit the objects in a world, ending up hitting a camera sensors. There are a number of resources on the web to build ray tracers, but I followed [The Ray Tracer Challenge](https://pragprog.com/book/jbtracer/the-ray-tracer-challenge).

Since most of the rays acting upon a scene don't hit the camera, the most efficient process is to see how rays _outgoing_ from a camera hit the objects in the world, and then see how those rays actually originated, taking into account multiple effects such as shadows, light reflection and diffusion, and world reflection on reflective surfaces.

All rays and objects are described in terms of euclidean space, the space of points and vectors, and transformations such as translate, rotate, scale. 

We will implement a simple ray tracer using ZIO environmental effects, to demonstrate how we can build a true onion architecture application, and test it completely relying solely on functional effects.

## 1. Make a camera
A camera in a 3d space is completely described by its resolution (`hRes`, `vRes`), the point where it is located (`viewFrom`), the point it is looking at (`viewTo`) and the direction of the vector "looking up" the camera, to distinguish how the camera is rotated. Moreover, the aperture angle of the camera determines uniquely the distance between the "eye" and the camera itself (see Fig 1)

![Definition of a camera](images/camera.png)

In terms of code we can create a camera through a method with this signature (`UIO` is an effect that can't fail and doesn't need any environment to be provided in order to run):

```scala
object Camera {
  def make(viewFrom: Pt, viewTo: Pt, upDirection: Vec, visualAngleRad: Double, hRes: Int, vRes: Int): UIO[Camera] = ???
}
```

Actually, it turns out handy to start from a camera in a standard position (looking at the negative z axis, being located in the origin, looking up on the y axis), which we will call _canonical camera_. Any camera in the world can be defined in terms of a transformation operated on the canonical camera. E.g. if I translate the camera to the point `Pt(0, 0, -5)` the transformation that must be carried along with our camera will be the transformation that must be applied to the world, in order to produce the same effect if the camera was the canonical one. That means if the real camera is positioned at `Pt(0, 0, -5)` it is the same thing to have a canonical camera, with the whole world translated by pf +5 on the `z` axis.

![Definition of a camera](images/camera-view-transform.png)

So we will start with the definition of a module that equips us with the capability of applying, and defining, these transformations, that are called _affine transformations_ and are completely specified by a matrix 4 x 4.

### 1.1. Define an Affine Transformations module
Affine transformations can be applied both to points and vectors. With the convention that a point `Pt(x, y, z)` is represented by a column vector $[x, y, z, 1]^T$, and a vector `Vec(x, y, z)` is represented by a vector $[x, y, z, 0]$, given an affine transformation matrix, applying that transformation to the vector or point means simply multiplying the transformation matrix by the vector representing the `Vec` or the `Pt`. To make things more efficient, given that often in our computations for every transformation we need the opposite one (think about changing perspective and observer relativity), we will compute the inverse at the moment of creation of an affine transformation.

```scala
abstract sealed case class AT private (direct: M, inverse: M) {
  def inverted: AT = AT(inverse, direct)
}
```

And then we need to define a module (i.e. a capability) that allows us to operate on these transformations. We will follow the ZIO convention in the module definition.

I always start with the companion object of the module, where the service is defined, here is an excerpt of our module methods

```scala
object ATModule {
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, AlgebraicError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def compose(first: AT, second: AT): ZIO[R, AlgebraicError, AT]
```