autoscale: true
footer: Pierangelo Cecchetto - @pierangelocecc 
slidenumbers: false

# Environmental effects

# A ray tracing exercise

<br/>
<br/>
<br/>

![right fit](img/title.png) 

---
[.build-lists: true]
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

# Functional Effects
^ for example let's consider a Matrix (value) and a function for adding 2 matrices. This function is defined for every possible pair of matrices, this means the function `add` is a TOTAL function

FP: Programming with values and functions

```scala
case class Matrix(x11: Double, x12: Double, x21: Double, x22: Double)

def add(m1: Matrix, m2: Matrix): Matrix
```

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

--


This function is not total. How do we deal with that?

---

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
