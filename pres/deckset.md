autoscale: true
footer: Pierangelo Cecchetto - @pierangelocecc 
slidenumbers: false
build-lists: true


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
1. Module pattern
1. Build Ray tracer components
1. Test Ray tracer components
1. Wiring things together
1. Make it work :nut_and_bolt:
1. Make it fast :rocket:

---
# Functional Effects

<br>

---

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

^ In this case we see that Option helps us dealing with one concern, i.e. the presence or absence of a given value (in our case the output of the `inverse` method)
Another concern is to express the presence the success of an operation (producing a value of type A) vs a failure (producing an error of type E)

# Functional Effects

Given a concern, build _**an immutable data structure**_ that provides a set of operations to deal with that concern

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
```scala
Option[A] 
Either[E, A]  
```

---

# Functional Effects
^ What do we do with these data structures? We can combine them (map, flatmap, zip), but ultimately we want to produce some workable value for our business case.
The interpretation phase takes these data structures, and extract the information and makes it usable

Interpretation: Produce some business value from the processing of these data structures

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-4]
[.code-highlight: 1-5]
```scala
val oa: Option[A]    
  oa.fold(ifEmpty)(a => ifNonEmpty) 

val ea: Either[E, A] 
  ea.fold(e => ifError)(a => ifSuccess)
```

---

![left fit](img/bread.jpg) 

# Functional Effects
^ Let's talk about one of my favourite hobbies. Baking bread. How do I express the act of baking, in an imperative way? I just give a list of instructions and they get immediately executed
Each of these statements gets executed immediately, or kicks in an asynchronous computation
How do we deal with this in a functional way? We separate the description from the execution, having a data type that _describes


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
^ How do we deal with this in a functional way? We separate the description from the execution, having a data type that _describes the computation
Then we provide this data type with (pretty standard) mechanism to chain/transform the computation, classical `map`/`flatMap`

![left fit](img/bread.jpg) 

#### Functional baking

[.code-highlight: none]
[.code-highlight: 1]
[.code-highlight: 1-2]
[.code-highlight: 1-3]
[.code-highlight: 1-8]
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
^ But how do I get by bread? I need to interpret the data structure

![left fit](img/bread.jpg) 

#### Functional baking

[.code-highlight: 1-2]
[.code-highlight: 1-5]
```scala
  def main() {
    val bakeBread: IO[Bread] = ???

    val bread: Bread = unsafeRun(bakeBread)   
  }
```

---

# Functional Effects
^ But how do I get by bread? I need to interpret the data structure

![left fit](img/bread.jpg) 

#### Functional baking

```scala
  def main() {
    val bakeBread: IO[Bread] = ???

    val bread: Bread = unsafeRun(bakeBread)   
  }
```

1. Build an immutable data structure to deal with side effects concern
1. Combine small data structures to more complex ones
1. Interpret the final data structure
1. Enjoy your bread

---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we add  salt too early and we kill the growth of our dough? What if our oven stops working halfway through this process?

![left fit](img/bread.jpg) 

#### Functional baking

Unaddressed concerns:

1. Errors


---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we cook too much our bread? What if our oven stops working halfway through? Well these are 2 different kinds of errors, one is what we call a failure (oven stops working), while the other one is what we call an error and we want to act upon (see talk from Francois Armand @ ScalaIO)

![left fit](img/bread.jpg) 

#### Functional baking

Unaddressed concerns:

1. Errors

```scala
  sealed trait BakingError     extends Exception

  case object WrongIngredients extends BakingError
  case object Overcooking      extends BakingError
```

---
[.build-lists: false]

# Functional Effects
^ There are 2 concerns we didn't cover yet in this exercise. What if we cook too much our bread? What if our oven stops working halfway through? Well these are 2 different kinds of errors, one is what we call a failure (oven stops working), while the other one is what we call an error and we want to act upon (see talk from Francois Armand @ ScalaIO)

![left fit](img/bread.jpg) 

#### Functional baking

Unaddressed concerns:

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

![left fit](img/bread.jpg) 

#### Functional baking

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


---
[.build-lists: false]

# Functional Effects
^ The second concern we need to deal with is the one of capabilities. What do I need in order to be able to knead my bread? What do I need to be able to cook it?
ZIO[R, E, A] is an immutable data structure that expresses the concern of running a computation that might produce a value A, or fail with an error E, and requires a set of capabilities `R` in order to run

![left fit](img/bread.jpg) 

#### Functional baking

Unaddressed concerns:

1. Errors :white_check_mark:
2. Capabilities

```scala
  ZIO[-R, +E, +A] // R => IO[E, A]

  val knead: ZIO[MixerEnv, WrongIngredients, Dough]
  val raise(dough: Dough): ZIO[WarmRoomEnv, Nothing, Dough]
  val cook(dough: Dough): ZIO[OvenEnv, Overcooking, Bread]
```

---
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
  runtime.unsafeRun(autonomousEffect)  
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
^ Not onluy ths works, but the compiler infers errors and environment for us
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

---


# Two dashes

The easiest way to build incremental slides is...
--
 to use two dashes `--` to separate content on a slide.

--

You can divide a slide in _any way you want_.

--

- One bullet

- Another bullet

--

- And one more

---

# Functional Effects
^ Now let's consider another function, that given a matrix, inverts it

FP: Programming with values and functions

```scala
def invert(m: Matrix): Matrix = ???

val m = Matrix( 
    1, 2, 
    1, 2
)

invert(m) // Exception!
```

This function is not total. How do we deal with that?
---


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

Manage your footnotes[^1] directly where you need them. Alongside numbers, you can also use text references[^Sample Footnote].

Include footnotes by inserting`[^Your Footnote]` within the text. The accompanying reference can appear anywhere in the document:

`[^Your Footnote]: Full reference here`

[^1]: This is the first footnote reference

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
