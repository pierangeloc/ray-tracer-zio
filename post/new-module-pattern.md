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