  autoscale: true
slidenumbers: false
build-lists: true
list: alignment(left)
image: background-color: #FF0000


# Environmental effects

# A ray tracing exercise

<br/>
<br/>
<br/>

![right fit](img/title.png) 

---
# Agenda

1. Functional Effects
1. Environmental effects
1x. Module pattern (we can skip it and make it in the next point)
1. Build Ray tracer components
1. Test Ray tracer components
1. Wiring things together
1. Make it work :nut_and_bolt:
1. Make it fast :rocket:

---
# Functional Effects

<br>


<!--
# Functional Effects

^ Let's talk about functional effects. As functional programmers we appreciate the benefits of programming only with functions and immutable values 

FP: Programming with values and functions

---
[.autoscale: false]
# Functional Effects
^ for example let's consider a Matrix (value) and a function for adding 2 matrices. This function is defined for every possible pair of matrices, this means the function `add` is a TOTAL function

FP: Programming with values and functions


[.code-highlight: 1-4]
[.code-highlight: 1-6]
[.code-highlight: 1-9]

```scala
case class Matrix(
  x11: Double, x12: Double, 
  x21: Double, x22: Double
)

def add(m1: Matrix, m2: Matrix): Matrix = ???

add(Matrix(1, 2, 3, 4), Matrix(5, 6, 7, 8)) 
// Matrix(6, 8, 10, 12)
```

---
[.autoscale: false]

# Functional Effects
^ Now let's consider another function, that given a matrix, inverts it

FP: Programming with values and functions

[.code-highlight: 1]
[.code-highlight: 1-5]
[.code-highlight: 1-7]

```scala
def invert(m: Matrix): Matrix = ???
val m = Matrix( 
    1, 2, 
    1, 2
)

invert(m) // Exception!
```

---
[.autoscale: false]

# Functional Effects
^ `invert` is clearly not a total function

`invert` is not a total function

[.code-highlight: 1]
[.code-highlight: 1-6]
```scala
def invert(m: Matrix): Option[Matrix] = ???
invert(
  Matrix(
    1, 2,
    1, 2)
  ) // None
```

---
-->

---

^ What is a functional effect? It is a data structure, plus some operations, to deal with a concern.
E.g. if our concern is to express the presence or absence of a value, a functional effect we are using daily is `Option[A]`
If our concern is expressing the success (producing a value of type A) vs a failure (producing an error of type E) of an operation we can use `Either[E,A]` 

# Functional Effects

Given a concern, build _**an immutable data structure**_ that provides a set of operations to deal with that concern[^1]

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
```scala
Option[A] 
Either[E, A]  
```

[^1]:  Semiquoting John De Goes (https://youtu.be/POUEz8XHMhE)

---

# Functional Effects
^ What do we do with these data structures? We can combine them (map, flatmap, zip), but ultimately we want to produce some workable value for our business case. Usually we us pattern matching, but we'd better get used to `fold`.
The interpretation phase takes these data structures, and extract the information and makes it usable

Interpretation: Produce some outcome from the processing of these data structures

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-4]
[.code-highlight: 1-5]
```scala
val oa: Option[A]    
val res: B = oa.fold[B](ifEmpty)(a => ifNonEmpty) 

val ea: Either[E, A] 
val res: B = ea.fold[B](e => ifError)(a => ifSuccess)
```

---

![left fit](img/bread.jpg) 

# Functional Effects
^ Let's talk about one of my favourite hobbies. Baking bread. How do I express the act of baking, in an imperative way? 
I just give a list of instructions and they get immediately executed
How do we deal with this in a functional way? We separate the description from the execution

#### Imperative baking

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-3]
[.code-highlight: 1-5]

```scala
def bakeBread(): Unit = {
  knead()
  raise()
  cook()
}
```
---

# Functional Effects
^ How do we deal with this in a functional way? We separate the description from the execution, having a data type that _describes_ what we want to do
Then we provide this data type with (pretty standard) mechanism to chain/transform the computation, classical `map`/`flatMap`

<!-- ![original](img/bread.jpg)  -->

#### Functional baking

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-3]
[.code-highlight: 1-9]
```scala
val knead: IO[Dough]
val raise(dough: Dough): IO[Dough]
val cook(dough: Dough): IO[Bread]

val bakeBread: IO[Bread] = for {
  d1 <- knead
  d2 <- raise(d1)
  b  <- cook(d2)
} yield b
```

---

# Functional Effects
^ ...and finally we get our bread by interpreting the data structure we just built.
It _traverses_ the data structure, or our recipe, and executes the instructions written on those data structures

<!-- ![left fit](img/bread.jpg)  -->

#### Functional baking

[.code-highlight: 1-2]
[.code-highlight: 1-5]
```scala
  def main() {
    val bakeBread: IO[Bread] = ???

    val bread: Bread = unsafeRun(bakeBread)   
  }







⠀
```

---
# Functional Effects
^ But how do I get by bread? I need to interpret the data structure

[.list: alignment(left)]
<!-- ![left fit](img/bread.jpg)  -->

#### Functional baking

```scala
  def main() {
    val bakeBread: IO[Bread] = ???

    val bread: Bread = unsafeRun(bakeBread)   
  }
```

1. Build an immutable data structure
1. Combine small data structures to more complex ones
1. Interpret the final data structure

<!-- 1. Enjoy your bread -->

<!-- ---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we put too much water in our dough? Can we recover from it? Should we retry somehow?

<!-- ![left fit](img/bread.jpg)  -->

<!-- ---

#### Functional baking
[.list: alignment(left)]

Errors -->

---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we cook too much our bread? What if our oven stops working halfway through? Well these are 2 different kinds of errors, one is what we call a failure (oven stops working), while the other one is what we call an error and we want to act upon (see talk from Francois Armand @ ScalaIO)

<!-- ![left fit](img/bread.jpg)  -->

#### Functional baking
[.list: alignment(left)]

Errors

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-4]
[.code-highlight: 1-7]
[.code-highlight: 1-10]

```scala
  sealed trait BakingError     extends Exception

  case object WrongIngredients extends BakingError
  case object Overcooking      extends BakingError

  val knead: IO[WrongIngredients, Dough] 
  val cook(dough: Dough): IO[Overcooking, Bread]
  
  val bake: IO[BakingError, Bread] =
    knead.flatMap(cook)  
```

---

<!-- [.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we cook too much our bread? What if our oven stops working halfway through? Well these are 2 different kinds of errors, one is what we call a failure (oven stops working), while the other one is what we call an error and we want to act upon (see talk from Francois Armand @ ScalaIO)

#### Functional baking
[.list: alignment(left)]

1. Errors

```scala
  sealed trait BakingError     extends Exception

  case object WrongIngredients extends BakingError
  case object Overcooking      extends BakingError

  val knead: IO[WrongIngredients, Dough] 
  val cook(dough: Dough): IO[Overcooking, Bread]
```

---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we cook too much our bread? What if our oven stops working halfway through? Well these are 2 different kinds of errors, one is what we call a failure (oven stops working), while the other one is what we call an error and we want to act upon (see talk from Francois Armand @ ScalaIO)


#### Functional baking
[.list: alignment(left)]

Unaddressed concerns:

1. Errors

```scala
  sealed trait BakingError     extends Exception

  case object WrongIngredients extends BakingError
  case object Overcooking      extends BakingError
  
  val knead: IO[WrongIngredients, Dough] 
  val cook(dough: Dough): IO[Overcooking, Bread]
  
  val bake: IO[BakingError, Bread] =
    knead.flatMap(cook)  
```

--- -->

[.build-lists: false]

# Functional Effects
^ The second concern we need to deal with is the one of capabilities. What do I need in order to be able to knead my bread? What do I need to be able to cook it?
* ZIO[R, E, A] is an immutable data structure that expresses the concern of running a computation that might produce a value A, or fail with an error E, and requires a set of capabilities `R` in order to run
* So I define the methods in terms of the capabilities and then I chain them through the monadic operators. Notice that the contravariance in `R` is all the compiler need to do the `with` between environments

<!-- ![left fit](img/bread.jpg)  -->

#### Functional baking
[.list: alignment(left)]

Capabilities

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-5]
[.code-highlight: 1-7, 9-13]
[.code-highlight: 1-13]
```scala
  ZIO[-R, +E, +A] // R => IO[E, A]

  val knead:               ZIO[MixerEnv, WrongIngredients, Dough] = ???
  def raise(dough: Dough): ZIO[WarmRoomEnv, Nothing, Dough] = ???
  def cook(dough: Dough):  ZIO[OvenEnv, Overcooking, Bread] = ???

  val bread
    : ZIO[OvenEnv with WarmRoomEnv with MixerEnv, BakingError, Bread]
  = for {
    dough <- knead
    risen <- raise(dough)
    ready <- cook(risen)
  } yield ready
```

---
<!-- [.build-lists: false] -->

# Functional Effects
^ZIO effects must be interpreted, before that point they are just simple data structures. Default runtime environemnt can cope with effects that require standard JVM capabilities (console, random, system, clock and blocking), but if we have an effect that has richer requirements we must satisfy those requirements before running it.
So we provide our bread baking instructions with all the requirements they have, and finally we are able to interpret our data structure and get our bread.
The problem with this is that it's not immediate (not to me at least) how to make non-trivial applications, in a layered way, with dependencies of layer upon layer, or even with circular dependencies?

#### Provide and run

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-3]
[.code-highlight: 1-5]
```scala
  val bread: ZIO[OvenEnv with WarmRoomEnv with MixerEnv, BakingError, Bread] = ???

  val r: ZIO[Any, BakingError, Bread] = bread.provide(new OvenEnv with WarmRoomEnv with MixerEnv)
  
  val result:Bread = runtime.unsafeRun(bread)
```

- Nice, but how do we deal with non-trivial applications?

- TODO: basic intro just to ZIO, R, E, A, access, accessM, provide, unsafeRun. Just use console and clock.

<!-- ---
[.build-lists: false]

# Environmental effects
^ What are environmental effects? They are functional effects (= immutable data structures) that model, at once, the requirement of an environment, the possibility to fail or succeed, and the possiblity to perform IO
This simple data type allows us to express different things: 
1. The introduction of an environment requirement. I can access the mixing machine together with the oven through that R


### `ZIO[-R, +E, +A]`

[.code-highlight: none]
[.code-highlight: 1-5]
[.code-highlight: 1-10]
```scala
  object ZIO {
    //environment introduction
    def access[R, E, A](f: R => A): ZIO[R, Nothing, A]
    def accessM[R, E, A](f: R => ZIO[R, E, A]): ZIO[R, E, A]
  }
```

---

# Environmental effects
^ What are environmental effects? They are functional effects (= immutable data structures) that model, at once, the requirement of an environment, the possibility to fail or succeed, and the possiblity to perform IO
This simple data type allows us to express different things: 
1. The introduction of an environment requirement. I can access the environemnt that provides me with the mixing capabilities


### `ZIO[-R, +E, +A]`

Environment introduction 

[.code-highlight: 1-5]
[.code-highlight: 1-9]
[.code-highlight: 1-13]
[.code-highlight: 1-18]
```scala
  object ZIO {
    def access[R, E, A](f: R => A): ZIO[R, Nothing, A]
    def accessM[R, E, A](f: R => ZIO[R, E, A]): ZIO[R, E, A]
  }

  trait MixingMachine {
    def knead: ZIO[Any, WrongIngredients, Dough] //IO[WrongIngredients, Dough]
  }

  trait MixerEnv {
    val mixingMachine: MixingMachine
  }

  val knead: ZIO[MixerEnv, WrongIngredients, Dough] =
    ZIO.accessM { mixerEnv =>
      mixerEnv.mixingMachine.knead
    }
```

---

# Environmental effects
^ And then, later (typically close to the main program, or in testing) I can provide the environment that my functional effect needs, eliminating the requirement of that environment 

### `ZIO[-R, +E, +A]`

Environment elimination

[.code-highlight: none]
[.code-highlight: 1-4]
[.code-highlight: 1-9]
```scala
  trait ZIO[-R, +E, +A] {
    // environment  elimination
    def provide(r: R): ZIO[Any, E, A]
  }

  val knead: ZIO[MixerEnv, WrongIngredients, Dough]
  
  val autonomousEffect = knead.provide(new MixerEnv{})
  val dough: Dough = runtime.unsafeRun(autonomousEffect)
```

---

# Environmental effects
^ How do we mix these capabilities and error together? The simplest thing we can do is just flatmap these structures 

![left fit](img/bread.jpg) 

#### Functional baking

Chaining errors and capabilities

```scala
  val knead:               ZIO[MixerEnv, WrongIngredients, Dough]
  val raise(dough: Dough): ZIO[WarmRoomEnv, Nothing, Dough]
  val cook(dough: Dough):  ZIO[OvenEnv, Overcooking, Bread]

  val bakeBread = for {
    d1 <- knead
    d2 <- raise(d1)
    b  <- cook(d2)
  } yield b 
```

---

# Environmental effects
^ Not only ths works, but the compiler infers errors and environment for us
We can see that the act of chaining these operations makes the required capabilities mix into an intersection type between
the capabilities (`type BakingEnv = MixerEnv with WarmRoomEnv with OvenEnv`), and it tries to unify the errors, looking for the nearest common supertype of errors, in our case `BakingError`


![left fit](img/bread.jpg) 

#### Functional baking

Chaining errors and capabilities

```scala
  val knead:               ZIO[MixerEnv, WrongIngredients, Dough]
  val raise(dough: Dough): ZIO[WarmRoomEnv, Nothing, Dough]
  val cook(dough: Dough):  ZIO[OvenEnv, Overcooking, Bread]

  type BakingEnv = MixerEnv with WarmRoomEnv with OvenEnv
  
  val bakeBread: ZIO[BakingEnv, BakingError, Bread] = for {
    d1 <- knead
    d2 <- raise(d1)
    b  <- cook(d2)
  } yield b 
```

Full inference of Environment and errors
 -->
---
![left fit](img/rt-world-light-eye.png) 

# Ray tracing
^The problem we want to solve is rendering a scene by simulating how the light works when coming from a light source, or from an environment, and hits some objects in an environemnt (world)
and finally hits the sensors in a camera, or the photosensitive cells in our retina

[.list: alignment(left)]
- Spheres (world), light source, eye

---
![left fit](img/rt-incident-rays.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    

---

^ but.. how do we build an image, ultimately?

![left fit](img/rt-reflected-rays.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    
- Reflected rays

---
^To build an image we need a canvas, a rectangular surface divided in pixels where the rays coming from the world will hit and produce the color they carry in the ray

![left fit](img/rt-reflected-rays-screen.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    
- Reflected rays
- Canvas

---

![left fit](img/rt-reflected-rays-screen-red-pixel.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    
- Reflected rays
- Canvas

---

![left fit](img/rt-reflected-rays-screen-red-green-pixel.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    
- Reflected rays
- Canvas

---

![left fit](img/rt-reflected-rays-screen-red-green-pixel-and-discarded.png) 

[.build-lists: false]

# Ray tracing
[.list: alignment(left)]

- Spheres (world), light source, eye
- Incident rays    
- Reflected rays
- Canvas
- Discarded rays

---
^At this point we have 2 options, one is computing all the rays for all the objects, and then consider only those that hit the canvas
Another option is work on the reverse problem, i.e. have rays going out of the canvas, hitting the objects in the world and determine how they behave considering all the agents

![left fit](img/rt-reflected-rays-screen-red-green-pixel-and-discarded.png) 

# Ray tracing

Options:

[.list: alignment(left)]
1. Compute all the rays (and discard most of them)
1. Compute only the rays outgoing the canvas, and determine how they behave on the surfaces

---
^Let's start building our model. A ray is an infinite line with a starting point. So let's start with the components we need to represent it, i.e. points and vectors

![left fit](img/rt-ray-def-tex.png) 

# Ray tracing

A ray is defined by the point it starts from, and its direction

$$
P(t) = P_0 + t \vec{D},   t > 0 
$$

---

# Minimal theory
^ We need to cover a bit of theory here to be able to cope with the fact we are in a 3D space where objects and rays are living
So we will cover points and vectors, and affine transformations, which are just translation/scaling/rotation and their combinations

![left fit](img/pt-vec-combined.png) 

[.list: alignment(left)]
- Points and Vectors
- Affine transformations

---
^ * Vectors are tuples of 3 coordinates, they can be added, every vector has an opposite, and there's a vector that is neutral element
* Points are also tuples of 3, representing the position of a point with respect to a reference frame. I can't add 2 points, but subtracting 2 points gives mme a vector. In particular, a vector has infinite representations in terms of pairs of points

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

Properties

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

---
^once we define points and vectors, the definition of a ray is immediate:

```scala
case class Ray(origin: Pt, direction: Vec) {
  def positionAt(t: Double): Pt = origin + (direction * t)
}
```

---
^ The transformations we are interested in are not exotic things, but the minimal requirements we might ask from something representing the reality, i.e. translations, rotations, scaling of vectors and points. At the end all of this boils down to multiplying matrices.
So provided 
* we have an AT type for our affine transformation 
* let's write our first ZIO module that allows us to operate on affine transformations, so we start defining a Service to apply them to point, vectors, and chain/compose them. The methods have a generic capability R required, in order to run
* Define a trait that has a val with the same name of the module, pointing to the service we just created
* Define an object extending the Service, parameterized on the module itself. This allows us to build from anywhere an effect that has as a requirement our module itself
* This process is very repetitive and mechanical, so an annotation can do the boring stuff for us (the same way simulacrum does for typeclasses)

### Affine transformations

[.code-highlight: 1]
[.code-highlight: 1, 7, 9-14]
[.code-highlight: 1, 3-5, 7, 9-14]
[.code-highlight: 1, 3-5, 7, 9-14, 16-26]
[.code-highlight: 1-5, 7, 9-14, 16-26]
[.code-highlight: 1-14, 16-26]
[.code-highlight: 1-26]
```scala
trait AT
/* Module */
trait ATModule {
  val aTModule: ATModule.Service[Any]
}

object ATModule {
  /* Service */
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, ATError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, ATError, Pt]
    def compose(first: AT, second: AT): ZIO[R, ATError, AT]
  }

  /* Accessor */
  object > extends Service[ATModule] {
    def applyTf(tf: AT, vec: Vec): ZIO[ATModule, ATError, Vec] =
      ZIO.accessM(_.aTModule.applyTf(tf, vec))
    def applyTf(tf: AT, pt: Pt): ZIO[ATModule, ATError, Pt] =
      ZIO.accessM(_.aTModule.applyTf(tf, pt))
    def compose(first: AT, second: AT): ZIO[ATModule, ATError, AT] =
      ZIO.accessM(_.aTModule.compose(first, second))
  }
}
```

---
### Affine transformations

[.code-highlight: 1-26]
```scala
trait AT
/* Module */
@accessible(">")
trait ATModule {
  val aTModule: ATModule.Service[Any]
}

object ATModule {
  /* Service */
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, ATError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, ATError, Pt]
    def compose(first: AT, second: AT): ZIO[R, ATError, AT]
  }

  /* Accessor is generated 
  object > extends Service[ATModule] {
    def applyTf(tf: AT, vec: Vec): ZIO[ATModule, ATError, Vec] =
      ZIO.accessM(_.aTModule.applyTf(tf, vec))
    def applyTf(tf: AT, pt: Pt): ZIO[ATModule, ATError, Pt] =
      ZIO.accessM(_.aTModule.applyTf(tf, pt))
    def compose(first: AT, second: AT): ZIO[ATModule, ATError, AT] =
      ZIO.accessM(_.aTModule.compose(first, second))
  }
  */
}
```

---
^ This accessor object allows us to summon our module wherever necessary
### Affine transformations

[.code-highlight: 1-26]
```scala
trait AT
/* Module */
@accessible(">")
trait ATModule {
  val aTModule: ATModule.Service[Any]
}

object ATModule {
  /* Service */
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, ATError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, ATError, Pt]
    def compose(first: AT, second: AT): ZIO[R, ATError, AT]
  }
}
```

---
^ This accessor object allows us to summon our module wherever necessary, and build something like this, where I'm summoning capabilities from different modules, not only, but the compiler is able to infer and mix these capabilities for us
### Affine transformations

[.code-highlight: 1-26]
```scala
val rotatedPt = 
  for {
    rotateX <- ATModule.>.rotateX(math.Pi / 2)
    _       <- Log.>.info("rotated of π/2")
    res     <- ATModule.>.applyTf(rotateX, Pt(1, 1, 1))
  } yield  res
```
---

^ This accessor object allows us to summon our module wherever necessary, and build something like this, where I'm summoning capabilities from different modules
### Affine transformations

[.code-highlight: 1-26]
```scala
val rotatedPt: ZIO[ATModule with Log, ATError, Pt] =
  for {
    rotateX <- ATModule.>.rotateX(math.Pi / 2)
    _       <- Log.>.info("rotated of π/2")
    res     <- ATModule.>.applyTf(rotateX, Pt(1, 1, 1))
  } yield  res
```
---


^ This accessor object allows us to summon our module wherever necessary, and build something like this
### Affine transformations

[.code-highlight: 1-26]
```scala
val rotatedPt: ZIO[ATModule with Log, ATError, Pt] = for {
  rotateX <- ATModule.>.rotateX(math.Pi / 2)
  _       <- Log.>.info("rotated of π/2")
  res     <- ATModule.>.applyTf(rotateX, Pt(1, 1, 1))
} yield  res
```

---

^ Now we want to provide an implementation of this module. Going back to the rotation example, with the conventions vec/pt => column matrix, all we need is being able to multiply matrices, so we have one dependency on another capability, the capability of multiplying matrices. 
### Affine transformations - Live

```scala
val rotated: ZIO[ATModule, ATError, Vec] = for {
  rotateX <- ATModule.>.rotateZ(math.Pi/2)
  res     <- ATModule.>.applyTf(rotateX, Vec(x, y, z))
} yield res
```

[.list: alignment(left)]
- `Vec(x, y, z)`  $$ \Rightarrow [x, y, z, 0]^T$$ 

- `Pt(x, y, z)`  $$ \Rightarrow [x, y, z, 1]^T$$ 

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

^ Given that we defined somewhere else another module to handle matrices
### Affine transformations - Live

[.code-highlight: 1-6]
[.code-highlight: 1-18]
```scala
// Defined somewhere else
object MatrixModule {
  trait Service[R] {
     def add(m1: M, m2: M): ZIO[R, AlgebraicError, M]
    def mul(m1: M, m2: M): ZIO[R, AlgebraicError, M]
  }
}

trait Live extends ATModule {
  val matrixModule: MatrixModule.Service[Any]

  val aTModule: ATModule.Service[Any] = new ATModule.Service[Any] {
    override def applyTf(tf: AT, vec: Vec): ZIO[Any, AlgebraicError, Vec] =
      for {
        col    <- PointVec.toCol(vec)
        colRes <- matrixModule.mul(tf, col)
        res    <- PointVec.colToVec(colRes)
      } yield res
```

---

^ Now that we have a live implementation of our AT, let's see if we can run our rotation
### Affine transformations - running

[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-6]
```scala
val rotated: ZIO[ATModule, ATError, Vec]  = ...
val program = rotatedPt.provide(new ATModule.Live{})
// Compiler error:
// object creation impossible, since value matrixModule 
// in trait Live of t ype matrix.MatrixModule.Service[Any] is not defined
// [error]   rotatedPt.provide(new ATModule.Live{})
```
---

^ Now that we have a live implementation of our AT, let's see if we can run our rotation
### Affine transformations - running

[.code-highlight: 1]
[.code-highlight: 1-5]
[.code-highlight: 1-7]
```scala
val rotated: ZIO[ATModule, ATError, Vec]  = ...
val program = rotatedPt.provide(
  new ATModule.Live with MatrixModule.BreezeLive
) 
// Compiles!
runtime.unsafeRun(program)
// Runs!
```
---
^So now we have a tool that allows us to perform the std transformations on our points and vectors, so we are ready to build a camera

### Layer 1: Affine transformations

![inline 80%](img/rotate-animated.gif)![inline 80%](img/translate-animated.gif)![inline 80%](img/scale-animated.gif)

---
^The first thing to consider is that everything is relative. If my standard position as observer is x=0, and I want to see how things look like from x = -3, what I can do is translate the world of +3 and keep on sitting at x = 0. We call this the canonical position
### Camera 

##### Everything is relative!

![inline](img/camera-translate.png) 

- Observe always from `x = 0` and translate the world by `+3`

---
^In the same spirit, let's define our canonical camera

![left fit](img/canonical-camera.png) 

### Camera - canonical

[.code-highlight: 1-4, 6]
```scala
case class Camera (
  hRes: Int,
  vRes: Int,
  fieldOfViewRad: Double,
  tf: AT
)
```

---

### Camera - generic
^Then I can consider any other camera in terms of this canonical, applying to the world the opposite of the transformation that brought my canonical camear to become the actual camera I'm using

![left fit](img/rotated-camera.png) 

[.code-highlight: 1]
[.code-highlight: 1, 10-16]
[.code-highlight: 1-16]
[.code-highlight: 1-20]
```scala
object Camera {

  def viewTransform(from: Pt, to: Pt, up: Vec):
    ZIO[ATModule, AlgebraicError, AT] = for {
    // some prepration ...
    translateTf  <- ATModule.>.translate(-from.x, -from.y, -from.z)
    composed     <- ATModule.>.compose(translateTf,  orientationAT)
  } yield composed

  def make(
    viewFrom: Pt, 
    viewTo: Pt, 
    upDirection: Vec, 
    visualAngleRad: Double, 
    hRes: Int, 
    vRes: Int): 
    ZIO[ATModule, AlgebraicError, Camera] =
    viewTransform(viewFrom, viewTo, upDirection).map { 
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
    scale     <- ATModule.>.scale(radius, radius, radius)
    translate <- ATModule.>.translate(center.x, center.y, center.z)
    composed  <- ATModule.>.compose(scale, translate)
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
#### Rastering - Dummy

[.code-highlight: 1-4]
[.code-highlight: 1-9]
[.code-highlight: 1-18]
```scala
@accessible(">")
trait RasteringModule {
  val rasteringModule: RasteringModule.Service[Any]
}
object RasteringModule {
  trait Service[R] {
    def raster(world: World, camera: Camera): 
      ZIO[R, Nothing, ZStream[R, RayTracerError, ColoredPixel]]
  }

  trait AllWhiteTestRasteringModule extends RasteringModule {
    val rasteringModule: Service[Any] = new Service[Any] {
      def raster(world: World, camera: Camera): UIO[ZStream[Any, RayTracerError, ColoredPixel]] =
        UIO.succeed(for {
          x <- ZStream.fromIterable(0 until camera.hRes)
          y <- ZStream.fromIterable(0 until camera.vRes)
        } yield ColoredPixel(Pixel(x, y), Color.white))
    }
  }
```

---
^Now what do we need to provide a LIVE implementation of this? We need to be able to provide one ray for each pixel of the camera, and for each ray we need to compute the color. Let's introduce 2 modules with these responsibilities

### World Rendering - Top Down

- Camera module - Ray per pixel

[.code-highlight: none]
[.code-highlight: 1-5]
```scala
object CameraModule {
  trait Service[R] {
    def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[R, Nothing, Ray]
  }
}
```

- World module - Color per ray

[.code-highlight: none]
[.code-highlight: 1-5]
```scala
object WorldModule {
  trait Service[R] {
    def colorForRay(world: World, ray: Ray): ZIO[R, RayTracerError, Color]
  }
}
```

---
^And with these 2 modules we can provide a live implementation of the rastering logic. We declare dependencies on the *services* of our modules, and then we access them in the implmentation

### World Rendering - Top Down
#### Rastering - Live

[.code-highlight: 1-3]
[.code-highlight: 1-6, 11-14]
[.code-highlight: 1-20]
```scala
trait LiveRasteringModule extends RasteringModule {
  val cameraModule: CameraModule.Service[Any]
  val worldModule: WorldModule.Service[Any]

  override val rasteringModule: Service[Any] = new Service[Any] {
    override def raster(world: World, camera: Camera): UIO[ZStream[Any, RayTracerError, ColoredPixel]] = {
      val pixels: Stream[Nothing, (Int, Int)] = ???
      UIO(
        pixels.mapM{
          case (px, py) =>
            for {
              ray   <- cameraModule.rayForPixel(camera, px, py)
              color <- worldModule.colorForRay(world, ray)
            } yield data.ColoredPixel(Pixel(px, py), color)
        }
      )
    }
  }
}
```

---
^To unit test this we should mock the dependencies. The pure way of doing this, in tagless-final is to use state monad, or use a `Ref` to preload our mocks and track assertions on the calls that have been performed. This can be done  with ZIO as well, but the process is pretty standard, so standard that ZIO provides a feature to make this feel almost natural. The first thing to do is defining the logic we want to test, e.g. here we want to check all the colored pixels produced by a raster

[.build-lists: false]

### Test **LiveRasteringModule**
1 - Define the method under test

```scala
val world = /* prepare a world */
val camera = /* prepare a camera */

val appUnderTest: ZIO[RasteringModule, RayTracerError, List[ColoredPixel]] =
  RasteringModule.>.raster(world, camera)
    .flatMap(_.runCollect)
```

---
^The second step is make our dependencies mockable, just annotate them

[.build-lists: false]

### Test **LiveRasteringModule** 
2 - Annotate the modules as mockable

```scala
@mockable
trait CameraModule { ... }

@mockable
trait WorldModule { ... }
```

---
^Step nr 3: define your expectations. We expect rayForPixel, when called for pixel 0, 0 to return ray r1
And we expect `colorForRay` when called for ray r1, to return red

[.build-lists: false]

### Test `LiveRasteringModule` 
3 - Build the expectations

```scala
val rayForPixelExp: Expectation[CameraModule, Nothing, Ray] =
  (CameraModule.rayForPixel(equalTo((camera, 0, 0))) returns value(r1)) *>
  (CameraModule.rayForPixel(equalTo((camera, 0, 1))) returns value(r2))

val colorForRayExp: Expectation[WorldModule, Nothing, Color] = 
  (WorldModule.colorForRay(equalTo((world, r1, 5))) returns value(Color.red)) *>
  (WorldModule.colorForRay(equalTo((world, r2, 5))) returns value(Color.green))
```

---
^Let's go back to the code we wanted to test. We must take all the expectations, flatmap/zip them and build a managed environment for our environmental effect under test. We turn the expectations into managed environments, which are similar to the environments but they have also strong guarantees of executing a release step, no matter what. Think of them as a try with resources on steroids, that works on asynchronous code as well. We'll see shortly why we have to make these expectations `Managed`

### Test **LiveRasteringModule** 
4 - Build the environment for the code under test

[.code-highlight: 1-3]
[.code-highlight: 1-20]
```scala
val appUnderTest: ZIO[RasteringModule, RayTracerError, List[ColoredPixel]] =
  RasteringModule.>.raster(world, camera)
    .flatMap(_.runCollect)

appUnderTest.provideManaged(
  worldModuleExp.managedEnv.zipWith(cameraModuleExp.managedEnv) { (wm, cm) =>
    new LiveRasteringModule {
      override val cameraModule: CameraModule.Service[Any] = cm.cameraModule
      override val worldModule: WorldModule.Service[Any] = wm.worldModule
        }
      }
    )
```

---
^turn the expectations into managed environments, which are similar to the environments but they have also strong guarantees of executing a release step, no matter what. Think of them as a try with resources on steroids, that works on asynchronous code as well. We'll see shortly why we have to make these expectations `Managed`

### Test **LiveRasteringModule**
5 - Assert on the results

```scala
assert(res, equalTo(List(
  ColoredPixel(Pixel(0, 0), Color.red),
  ColoredPixel(Pixel(0, 1), Color.green),
  ColoredPixel(Pixel(1, 0), Color.blue),
  ColoredPixel(Pixel(1, 1), Color.white),
  ))
)
```

---
^Here's how the whole test looks like

### Test **LiveRasteringModule**

```scala
suite("LiveRasteringModule") {
  testM("raster should rely on cameraModule and world module") {
    val camera = Camera.makeUnsafe(Pt.origin, Pt(0, 0, -1), Vec.uy, math.Pi / 3, 2, 2)
    val world = World(PointLight(Pt(5, 5, 5), Color.white), List())
    val appUnderTest: ZIO[RasteringModule, RayTracerError, List[ColoredPixel]] =
      RasteringModule.>.raster(world, camera).flatMap(_.runCollect)

    for {
      (worldModuleExp, cameraModuleExp) <- RasteringModuleMocks.mockExpectations(world, camera)
      res <- appUnderTest.provideManaged(
        worldModuleExp.managedEnv.zipWith(cameraModuleExp.managedEnv) { (wm, cm) =>
          new LiveRasteringModule {
            override val cameraModule: CameraModule.Service[Any] = cm.cameraModule
            override val worldModule: WorldModule.Service[Any] = wm.worldModule
              }
            }
          )
      } yield assert(res, equalTo(List(
          ColoredPixel(Pixel(0, 0), Color.red),
          ColoredPixel(Pixel(0, 1), Color.green)
          )))
  }
}
```

---
^So the takeaway of this is...

### Test

#### Takeaway: Implement and test every layer only in terms of the immediately underlying layer

---
^And now that we got warmed up with this, let's go on and implement all the logic through modules

## Modules all the way down

---
^Let's give an implementation to our CameraModule. I don't want to go through this in depth but remember that we compute the rays for a canonical camera, whose rays all start at (0, 0, 0) and look at (0, 0, -1) 
Implementation: the canonical camera has the eye in Pt.origin, and the screen on the plane z = -1,
therefore after computing the coordinates of the point in the screen, we have to apply the _inverse of the camera transformation_
because the camera transformation is the transformation to be applied to thw world in order to produce the effect of moving/orienting the camera around
This transformation must be applied both to the point in the camera, and to the origin. Then the computation of the ray is trivial.

### Live **CameraModule**
```scala
trait Live extends CameraModule {
  val aTModule: ATModule.Service[Any]

  val cameraModule: CameraModule.Service[Any] = new Service[Any] {
    
    override def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[Any, Nothing, Ray] =
      for {
        xOffset   <- UIO((px + 0.5) * camera.pixelXSize)
        yOffset   <- UIO((py + 0.5) * camera.pixelYSize)
        //coordinates of the canvas point before the transformation
        origX     <- UIO(camera.halfWidth - xOffset)
        origY     <- UIO(camera.halfHeight - yOffset)
        //transform the coordinates by the inverse
        inverseTf <- aTModule.invert(camera.tf)
        pixel     <- aTModule.applyTf(inverseTf, Pt(origX, origY, -1))
        origin    <- aTModule.applyTf(inverseTf, Pt.origin)
        direction <- (pixel - origin).normalized.orDie
      } yield Ray(origin, direction)
  }
```

---
^To provide a color for a ray, first thing to do is to see if that ray hits something. This responsibility is delegated to another module, responsible to deal with the topological structure of our world. 
* Let's define an intersection for a ray that hits a given shape

### Live **WorldModule**
[.code-highlight: 1]
[.code-highlight: 1-9]
[.code-highlight: 1-10]
[.code-highlight: 1-12]
[.code-highlight: 1-15]
```scala
case class Intersection(t: Double, sceneObject: Shape) 

trait Live extends WorldModule {
  val worldTopologyModule: WorldTopologyModule.Service[Any]

  override val worldModule: Service[Any] = new Service[Any] {
    def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[Any, RayTracerError, Color] =
      for {
        intersections <- worldTopologyModule.intersections(world, ray)
        maybeHitComps <- intersections.find(_.t > 0).traverse(i => hitComps(i))
        color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) { hc =>
          worldTopologyModule.isShadowed(world, hc.overPoint).flatMap(process(hc, _))
        }
        /* ... */
      } yield color
```

---
^Let's look directly at the Live implementation of the `WorldTopologyModule`
* For the intersections, we need to traverse all the objects of the world, and look for the intersection between that object and the ray. This is delegated to the ray module that deals with all the possible shapes we want to handle (atm planes and spheres, but we can add cylinders, triangles, etc). I think you got the mechanism by now
* Topology is also about finding if a point is shadowed by another shape, and for this we take the vector that goes from the point intersected by the ray, pointing towards the light source, and see if that ray has intersections. If it has, the point is in shadow, otherwise it is clear.

### Live **WorldModule**
#### `WorldTopologyModule`
![left fit](img/shadow.png) 

[.code-highlight: 6-7]
[.code-highlight: 1-7]
[.code-highlight: 1-11]
[.code-highlight: 1-18]
```scala
trait Live extends WorldTopologyModule {
  val rayModule: RayModule.Service[Any]

  override val worldTopologyModule: Service[Any] = new Service[Any] {

    def intersections(world: World, ray: Ray): ZIO[Any, Nothing, List[Intersection]] =
      ZIO.traverse(world.objects)(rayModule.intersect(ray, _)).map(_.flatten.sortBy(_.t))

    def isShadowed(world: World, pt: Pt): ZIO[Any, Nothing, Boolean] =
      for {
        v        <- UIO(world.pointLight.position - pt)
        distance <- v.norm
        vNorm    <- v.normalized.orDie
        xs       <- intersections(world, Ray(pt, vNorm))
        hit      <- rayModule.hit(xs)
      } yield hit.exists(i => i.t > 0 && i.t < distance)
  }
} 
```

---
^ If we go back to our world module, the next thing to do is finding the hit components 
### Live **WorldModule**
```scala
case class Intersection(t: Double, sceneObject: Shape) 

trait Live extends WorldModule {
  val worldTopologyModule: WorldTopologyModule.Service[Any]
  val worldHitCompsModule: WorldHitCompsModule.Service[Any]

  override val worldModule: Service[Any] = new Service[Any] {
    def colorForRay(world: World, ray: Ray, remaining: Int = 5): ZIO[Any, RayTracerError, Color] =
      for {
        intersections <- worldTopologyModule.intersections(world, ray)
        maybeHitComps <- intersections.find(_.t > 0).traverse(i => worldHitCompsModule.hitComps(i))
        color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) { hc =>
          worldTopologyModule.isShadowed(world, hc.overPoint).flatMap(process(hc, _))
        }
        /* ... */
      } yield color
```


---
^ Let's implement the hit components module. We need to be able to compute the normal to our shape in a given hit point, and we give this responsibility to a different module, the ` NormalReflectModule`
### Live `WorldModule`
#### `HitCompsModule`

![left fit](img/hit-components.png) 

```scala
case class HitComps(
  shape: Shape, hitPt: Pt, normalV: Vec, eyeV: Vec, rayReflectV: Vec
)

trait Live extends WorldHitCompsModule {
  val normalReflectModule: NormalReflectModule.Service[Any]

  val worldHitCompsModule: WorldHitCompsModule.Service[Any] = new Service[Any] {
    def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]):
     ZIO[Any, GenericError, HitComps] =
    for {
      pt       <- UIO(ray.positionAt(hit.t))
      normalV  <- normalReflectModule.normal(pt, hit.sceneObject)
      eyeV     <- UIO(-ray.direction)
      reflectV <- normalReflectModule.reflect(ray.direction, normalV)
    } yield HitComps(hit.sceneObject, pt, normalV, eyeV, reflectV)
  }
}
```

---
^ So going back to our WorldModule, first we get the intersections
* then we get the hit components. If no hit, display black
* If hit, calculate the hit components (those 4 vectors we saw before), determine if the point is shadowed, and then deterine the color. To determine the color, let's introduce another module that, from the hit components, can determine the color to be displayed for our ray. 
Usual procedure: Add the dependency, use it, provide an implementation. We use for this the Phong reflection model
### Determine the color
#### `PhongReflectionModule`


[.code-highlight: 1-10]
[.code-highlight: 1-12]
[.code-highlight: 1-13]
[.code-highlight: 1-24]
```scala
trait Live extends WorldModule {
    val worldTopologyModule: WorldTopologyModule.Service[Any]
    val worldHitCompsModule: WorldHitCompsModule.Service[Any]
    val phongReflectionModule: PhongReflectionModule.Service[Any]

    override val worldModule: Service[Any] = new Service[Any] {
      def colorForRay(world: World, ray: Ray, remaining: Int = 5):
        ZIO[Any, RayTracerError, Color] =
        for {
          intersections <- worldTopologyModule.intersections(world, ray)
          maybeHitComps <- intersections.find(_.t > 0)
            .traverse(worldHitCompsModule.hitComps(ray, _, intersections))
          color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) {
            hc =>
              for {
                shadowed <- worldTopologyModule
                  .isShadowed(world, hc.overPoint)
                color <- phongReflectionModule
                  .lighting(world.pointLight, hc, shadowed).map(_.toColor)
              } yield color
          }
        } yield color
    }
  }
```

---
^The simplest implementation of the phong reflection model, is something that when in shadow displays black, and when in light displays white
### Determine the color
#### **PhongReflectionModule** - Dummy implementation

```scala
trait BlackWhite extends PhongReflectionModule {
  override val phongReflectionModule: Service[Any] = new Service[Any] {

    override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): UIO[PhongComponents] = {
      if (inShadow) UIO(PhongComponents.allBlack)
      else UIO(PhongComponents.allWhite)
    }
  }
}
```

---
^We have enough elements now to build a first version of our program
### Display the first canvas / 1

```scala
def drawOnCanvasWithCamera(world: World, camera: Camera, canvas: Canvas):
  ZIO[RasteringModule, RayTracerError, Unit] = 
  for {
    coloredPointsStream <- RasteringModule.>.raster(world, camera)
    _                   <- coloredPointsStream.mapM(cp => canvas.update(cp)).run(Sink.drain)
  } yield ()

def program(viewFrom: Pt):
  ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit] =
  for {
    camera <- cameraFor(viewFrom: Pt)
    w      <- world
    canvas <- drawOnCanvasWithCamera(w, camera)
    _      <- CanvasSerializer.>.serialize(canvas, 255)
  } yield ()
```

---
^If we try to provide environments to our program, we see that the compiler guides us to close the holes
### Display the first canvas / 2

```scala
def program(viewFrom: Pt):
  ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit]

program(Pt(2, 2, -10))
  .provide(
    new CanvasSerializer.PPMCanvasSerializer 
    with RasteringModule.ChunkRasteringModule 
    with ATModule.Live
  )
// Members declared in zio.blocking.Blocking
// [error]   val blocking: zio.blocking.Blocking.Service[Any] = ???
// [error]
// [error]   // Members declared in modules.RasteringModule.ChunkRasteringModule
// [error]   val cameraModule: modules.CameraModule.Service[Any] = ???
// [error]   val worldModule: modules.WorldModule.Service[Any] = ???
// [error]
// [error]   // Members declared in geometry.affine.ATModule.Live
// [error]   val matrixModule: geometry.matrix.MatrixModule.Service[Any] = ???
```

---
^We can close the holes bit by bit following the compiler
I find this more readable than implicits not found thrown when a typeclass lookup is not successful when adopting tagless final technique
### Display the first canvas / 3

```scala
def program(viewFrom: Pt):
  ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit]

program(Pt(2, 2, -10))
  .provide(
    new CanvasSerializer.PPMCanvasSerializer 
    with RasteringModule.ChunkRasteringModule 
    with ATModule.Live 
    with CameraModule.Live 
    with MatrixModule.BreezeLive 
    with WorldModule.Live
    )
  )
// [error]   // Members declared in io.tuliplogic.raytracer.ops.model.modules.WorldModule.Live
// [error]   val phongReflectionModule: io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.Service[Any] = ???
// [error]   val worldHitCompsModule: io.tuliplogic.raytracer.ops.model.modules.WorldHitCompsModule.Service[Any] = ???
// [error]   val worldReflectionModule: io.tuliplogic.raytracer.ops.model.modules.WorldReflectionModule.Service[Any] = ???
// [error]   val worldRefractionModule: io.tuliplogic.raytracer.ops.model.modules.WorldRefractionModule.Service[Any] = ???
// [error]   val worldTopologyModule: io.tuliplogic.raytracer.ops.model.modules.WorldTopologyModule.Service[Any] = ???
```

---
^One nice thing of using simple intersection types is that we can create modules that satisfy multiple dependencies, e.g.
### Display the first canvas - /4

Group modules in **trait**

```scala
trait BasicModules
  extends NormalReflectModule.Live
  with RayModule.Live
  with ATModule.Live
  with MatrixModule.BreezeLive
  with WorldModule.Live
  with WorldTopologyModule.Live
  with WorldHitCompsModule.Live
  with CameraModule.Live
  with RasteringModule.Live
  with Blocking.Live
```

---
^Given that definition, we can just provide our program with the missing module, which is the phong reflection module, and we are done
### Display the first canvas - /4

Group modules

```scala
def program(viewFrom: Pt):
  ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit]

program(Pt(2, 2, -10))
  .provide(new BasicModules with PhongReflectionModule.BlackWhite)
```

---
^We can now see if we produce something slightly meaningful, running in sequence a program for different points of view
### Display the first canvas - /4
![left fit](img/simple-world-shadows-anim.gif) 

Group modules

```scala
def program(viewFrom: Pt):
  ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit]

override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    ZIO.traverse(-18 to -6)(z => program(Pt(2, 2, z))
      .provide(
        new BasicModules with PhongReflectionModule.BlackWhite
      )
    ).foldM(err =>
      console.putStrLn(s"Execution failed with: $err").as(1),
      _ => UIO.succeed(0)
    )
```

---
^So far we didn't look at the color of objects, or their material properties, but that's what makes the colors of an object. So we define a data type that collects the material properties that make how it looks like
*Ambient models the presence of an "ambient" light uniformly distributed in the world
*Diffusion models the behavior of light hitting a matte material and assumes that the effect of light hitting a material at a give point is just depending on the projection of the ray of light on the normal vector to the surface at that point. This will affect any eye observing that point in LOS.
*Specularity/shininess model how the source of light is reflected by the material, i.e. how you are going to see that lamp reflected on the object itself.


![left fit](img/specular-shininess.png) 
### Color the scene

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
^Without diving into further details, we implement handling of these components in terms of separate modules

```scala
trait Live extends PhongReflectionModule {
  val aTModule: ATModule.Service[Any]
  val normalReflectModule: NormalReflectModule.Service[Any]
  val lightDiffusionModule: LightDiffusionModule.Service[Any]

override val phongReflectionModule: Service[Any] = new Service[Any] {
  override def lighting(
    pointLight: PointLight, hitComps: HitComps, inShadow: Boolean
  ): UIO[PhongComponents] = {
    /* ... */
    for {
      color          <- colorAtSurfacePoint
      effectiveColor <- UIO.succeed(color * pointLight.intensity)
      ambient        <- UIO(PhongComponents.ambient(
        effectiveColor * hitComps.shape.material.ambient)
      )
      res            <- if (inShadow) UIO(PhongComponents.allBlack) 
        else diffuse(effectiveColor)
    } yield ambient + res
  }
```

---
^Without diving into further details, we implement handling of these components in terms of separate modules

![left fit](img/phong-animated.gif) 

#### Use the live module

[.code-highlight: 1-1]
[.code-highlight: 1-2]
[.code-highlight: 1-3]
[.code-highlight: 1-6]
```scala
program(Pt(2, 2, -10))
  .provide(
    new BasicModules 
    with PhongReflectionModule.Live
    // with PhongReflectionModule.BlackWhite
  )
```

---
^ With what we developed so far (plus some tricks such as replacing the simple color with a pattern, but they don't need anything more than just affine transformations) we are able to put together a scene like this, which is not too bad. 
Let's add a `reflective` parameter and put it into use  

![left fit](img/three-spheres-opaque.png) 


#### Render 3 spheres - reflect light source

[.code-highlight: 1-6]
[.code-highlight: 1-6, 8-14]
[.code-highlight: 1-6, 8-14, 15-18]
[.code-highlight: 1-6, 8-14, 15-18, 19-25]
```scala
case class Material(
  pattern: Pattern, // the color pattern
  ambient: Double,  // ∈ [0, 1] 
  diffuse: Double,  // ∈ [0, 1]
  specular: Double, // ∈ [0, 1]
  shininess: Double, // ∈ [10, 200]
  reflective: Double, // ∈ [0, 1]
)

trait Live extends PhongReflectionModule {
  /* other modules */
  val lightReflectionModule: LightReflectionModule.Service[Any]
}

trait RenderingModulesV1
  extends PhongReflectionModule.Live
  with LightDiffusionModule.Live

program(
  from = Pt(57, 20, z),
  to = Pt(20, 0, 20)
).provide {
  new BasicModules 
  with RenderingModulesV1
}
```

---
^In our hit components we have already computed the reflected ray. All we have to do is compute how that reflected ray sees the world, find the color, multiply it by the `reflective` parameter of our material, and add it to the natural color of the object that we computed so far. The computation is delegated to the `WorldReflectionModule`

![left fit](img/hit-components.png) 

#### Handling reflection

Pimp up the `WorldModule`

[.code-highlight: 1-3]
[.code-highlight: 1-13]
[.code-highlight: 1-14]
[.code-highlight: 1-19]
```scala
trait Live extends WorldModule {
  /* old modules required */
  val worldReflectionModule: WorldReflectionModule.Service[Any]

  override val worldModule: Service[Any] = new Service[Any] {
    def colorForRay(world: World, ray: Ray): ZIO[Any, RayTracerError, Color] =
      for {
        intersections <- /* find intersections */
        maybeHitComps <- /* find hit components? */
        color <- maybeHitComps.fold[IO[RayTracerError, Color]](UIO(Color.black)) {
          hc =>
            for {
              color <- /* standard computation of color */  
              reflectedColor <- worldReflectionModule.reflectedColor(world, hc) 
            } yield color + reflectedColor + refractedColor
        }
      } yield color
  }
}
```

---
^The WorldReflectionModule is responsible for computing the view of the world from a reflected ray perspective

![left fit](img/hit-components.png) 

#### Handling reflection - Live


[.code-highlight: 1, 4-9]
[.code-highlight: 1, 4-10]
[.code-highlight: 1, 4-11]
[.code-highlight: 1-16]
```scala
trait Live extends WorldReflectionModule {
  val worldModule: WorldModule.Service[Any]

  val worldReflectionModule = new WorldReflectionModule.Service[Any] {
    def reflectedColor(world: World, hitComps: HitComps, remaining: Int):
      ZIO[Any, RayTracerError, Color] =
      if (hitComps.shape.material.reflective == 0) {
        UIO(Color.black)
      } else {
        val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
        worldModule.colorForRay(world, reflRay, remaining).map(c =>
          c * hitComps.shape.material.reflective
        )
      }
  }
}
```

- Circular dependency! :muscle: :muscle:

---
^Calculating reflection can be pretty expensive, as it's like having a much higher number of observers for which we have to calculate all the rays. It can also introduce explosive behavior, like when you look at 2 mirrors face 2 face and you see the infinite, so we introduce also a mechanism to limit the recursion depth, but I don't talk about it here.
But considering it's expensive, it can be handy to provide an implementation of the reflectin module that does nothing and returns black. This can be useful if I want to first check how an image looks like approx and then later compute it in all its beauty

#### Handling reflection - Noop module
```scala
trait NoReflectionModule extends WorldReflectionModule {
  val worldReflectionModule = new WorldReflectionModule.Service[Any] {
    def reflectedColor(
      world: World, 
      hitComps: HitComps, 
      remaining: Int
    ): ZIO[Any, RayTracerError, Color] = UIO.succeed(Color.black)
  }
}
```

--- 

#### Handling reflection - Noop module

```scala
program(
  from = Pt(57, 20, z),
  to = Pt(20, 0, 20)
).provide {
  new BasicModules 
  with PhongReflectionModule.Live
  with 
}
```

---


^One nice characteristic of environmental effects is that they allow:
* Grouping environments by `with`
* Provide partial implementation. E.g. We will rarely change the Rastering/CanvasSerializer/CameraModule/MatrixModule/AtModule. We will likely change the implementation of our reflection/refraction etc
* and then we can swap stuff around achieving different visual effects

### Providing partial environments
---
^We can close the holes bit by bit following the compiler
I find this more readable than implicits not found thrown when a typeclass lookup is not successful when adopting tagless final technique
### Display the first canvas / 3
---
^We can close the holes bit by bit following the compiler
I find this more readable than implicits not found thrown when a typeclass lookup is not successful when adopting tagless final technique
### Display the first canvas / 3

---

### Affine Transformations
- Scale vectors and points




## Preparing slides for your class doesn’t have to be an endless chore.
## Here are a few Deckset features that will help you get the most out of your slides.

---

# Footers and Slide Numbers

Include persistent custom footers and/or running slide numbers by using directives:

```
footer: © Unsigned Integer UG, 2017
slidenumbers: true
```

Make sure the two directives start on the *first line* of your markdown file, and ensure there are *no empty lines* between the two.

---

# Footnotes

Manage your footnotes[^] directly where you need them. Alongside numbers, you can also use text references[^Sample Footnote].

Include footnotes by inserting`[^Your Footnote]` within the text. The accompanying reference can appear anywhere in the document:

`[^Your Footnote]: Full reference here`

[^2]: This is the first footnote reference

[^Sample Footnote]: This is the second footnote reference

---

# Footnotes

Footnote references need to be *unique in the markdown file*. This means, that you can also reference footnotes from any slide, no matter where they are defined.

When there are multiple references are listed, they must all be separated by blanks lines.

---


# Nested Lists

- You can create nested lists
    1. by indenting
    1. each item with
    1. 4 spaces
- It’s that simple

---

# Links

Create links to any external resource—like [a website](http://www.deckset.com)—by wrapping link text in square brackets, followed immediately by a set of regular parentheses containing the URL where you want the link to point:

`‘[a website](http://www.deckset.com)’`

Your links will be clickable in exported PDFs as well!

---

# Display formulas

Easily include mathematical formulas by enclosing TeX commands in `$$` delimiters. Deckset uses [MathJax](http://www.mathjax.org/) to translate TeX commands into beautiful vector graphics.

<a name="formulas"></a>

---

## Schrödinger equation

The simplest way to write the time-independent Schrödinger equation is $$H\psi = E\psi$$, however, with the Hamiltonian operator expanded it becomes:

$$
-\frac{\hbar^2}{2m} \frac{d^2 \psi}{dx^2} + V\psi = E\psi
$$

---

# Captioned Images and Videos

![inline](room.jpg)

Easily create captions using [inline] images/videos with text underneath.

---

# Plus:

- PDF export for printed handouts
- Speaker notes and rehearsal mode
- Switch theme and ratio on the fly
- Animated GIFs for cheap wins and LOLs :-)
