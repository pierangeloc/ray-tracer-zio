package io.tuliplogic.raytracer.ops.programs

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, LinkedBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}

import zio.clock.Clock
import zio.duration.Duration
import zio.internal.{Executor, NamedThreadFactory, PlatformLive}
import zio.stream._
import zio.{App, DefaultRuntime, IO, Schedule, UIO, ZEnvDefinition, ZIO, ZSchedule, blocking, clock, console}

object StreamApp extends App {

  Executors.newFixedThreadPool(32)
  override val Platform = PlatformLive.Default.withExecutor(Executor.fromThreadPoolExecutor(_ => 2048) {
    val threadPool = new ThreadPoolExecutor(
      32,
      32,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue[Runnable](),
      new ThreadFactory {
        val ctr = new AtomicInteger(0)
        def newThread(r: Runnable): Thread = {
          val thread = new Thread(r)
          thread.setName(s"piero-zio-thread-${ctr.getAndIncrement()}")
          thread.setDaemon(true)
          thread
        }
      }
    )
    threadPool.allowCoreThreadTimeOut(true)

    threadPool
  })

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (for {
      start <- clock.nanoTime
      _     <- str
      end   <- clock.nanoTime
      _     <- console.putStr(s"it took ${(end - start) / 1000000d} millis")
    } yield ()).foldM(err => console.putStrLn(s"Execution failed").as(1), _ => UIO.succeed(0))

  val str = Stream.fromIterable(0 to (640 * 480))//.mapM(processing(10))
    .chunkN(20000)
    .mapMPar(15)(chunk => chunk.mapM(processing(10)))
      .run(Sink.drain)

  def processing(micros: Int)(n: Int) = //blocking.blocking(
    ZIO.effectTotal {
      val start = System.nanoTime()
      while ((System.nanoTime() - start) < micros * 1000) {}
      0
    }
//  )

}


object SingleCoreApp extends scala.App {
  def processing(micros: Int)(n: Int) = {
          val start = System.nanoTime()
      while ((System.nanoTime() - start) < micros * 1000) {}
      0
  }

  scala.Stream.from(0, 6400 * 480).map(processing(10)).toList

}

object TestClock {
  import scala.concurrent.duration._
  case class SimpleEvent(at: Instant)
  ZStream.repeatEffect(
    clock.currentDateTime.map(at => SimpleEvent(at.toInstant))
  ).schedule(ZSchedule.spaced(zio.duration.Duration.fromScala(10.seconds)))

}

import zio.ZIO
import zio.stream.ZStream

object Repro extends zio.App {

  def stream: ZIO[Any, Nothing, List[Either[Int, Int]]] = ZStream
    .fromIterable[Int](Seq.empty)
    .partitionEither(i => ZIO.succeed(if (i % 2 == 0) Left(i) else Right(i)))
    .map { case (evens, odds) => evens.mergeEither(odds) }
    .use(_.runCollect)

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    ZIO.sequence(Range(0, 100).toList.map(i => for {
      res <- stream
        _ <- ZIO.effectTotal(println(s"$i, $res"))
    } yield ())).map(_ => 0)
//    stream.flatMap(list => ZIO.effectTotal(println(list))).as(0)
//    ZIO.sequence(Range(0, 100).toList.map(x => ZIO.effectTotal(println(x)))).as(0)
  }
}

object TST {
  sealed trait BakingError     extends Exception

  case class WrongIngredients() extends BakingError
  case class Overcooking()      extends BakingError

  trait Dough
  type Bread
  trait MixerEnv
  trait WarmRoomEnv
  trait OvenEnv

  val knead: ZIO[MixerEnv, WrongIngredients, Dough] = ???
  def raise(dough: Dough): ZIO[WarmRoomEnv, Nothing, Dough] = ???
  def cook(dough: Dough): ZIO[OvenEnv, Overcooking, Bread] = ???

  val bread: ZIO[OvenEnv with WarmRoomEnv with MixerEnv, BakingError, Bread]

  = for {
    dough <- knead
      risen <- raise(dough)
      ready <- cook(risen)
  } yield ready

  val r: IO[BakingError, Bread] = bread.provide(new OvenEnv with WarmRoomEnv with MixerEnv)
  new DefaultRuntime {}.unsafeRun(r)
}