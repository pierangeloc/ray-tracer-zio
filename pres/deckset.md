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
#### Rastering

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
^Now what do we need to provide a LIVE implementation of this? We need to be able to provide one ray for each pixel of the camera, plus

### World Rendering - Top Down
#### Camera module - One ray per pixel
<!-- #### World moduld -->

```scala
object CameraModule {
  trait Service[R] {
    def rayForPixel(camera: Camera, px: Int, py: Int): ZIO[R, AlgebraicError, Ray]
  }
```

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
