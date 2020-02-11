# Unleash ZIO environment with `ZLayer`

ZIO is designed around 3 parameters, `R, E, A`. `R` represents the _requirement_ for the effect to run, meaning we need to fulfill
the requirement in order to make the effect _runnable_. We will dedicate this post to explore what we can do with R, as it is paramount 
inthe ZIO architecture

## A Simple case
Let's consider the case where we want to build a database connect string from configuration, where the configuration is represented by a case class

```scala
case class DBConnection(...)
val productDetails: ZIO[DBConnection, Nothing, Product]
val relatedProducts: ZIO[DBConnection, Nothing, List[ProductId]]

val getProductWithRelated: ZIO[DBConnection, Nothing, (Product, List[ProductId])] = 
    productDetails zip relatedProducts 
```

Looking at the types above we see that the environment is a `DBConnection`, i.e. we must supply a `DBConnection` in order to run the effects, through `provide`

```scala
val dbConnection: DBConnection = ???
val runnable: ZIO[Any, Nothing, (Product, List[ProductId])] = getProductWithRelated.provide(dbConnection)

val (product, related)  = runtime.unsafeRun(runnable)
```

## Our first ZIO module
The module pattern is _the way_ ZIO manages dependencies between application components, and it works well with ZIO thanks to the presence of `R` in the `ZIO` datatype.
In its initial formulation it was only using trait mix-ins, as shown In [this talk](https://www.youtube.com/watch?v=IvL8mmB2RBM).

Here we will see the new formulation of the module pattern, that resolves some of the shortcomings of the previous version.

### The module recipe
Let's build a module for product data access, following these simple 3 steps:

1. Define an object that gives the name to the module, this can be (and typically is, in ZIO itself) a package object
1. Within the module object define a `trait Service` that defines the interface our module is exposing, in our case 2 methods to retrieve a product details, and one to retrieve the related products
1. Within the module object define a type alias like `type ModuleName = Has[Service]`



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