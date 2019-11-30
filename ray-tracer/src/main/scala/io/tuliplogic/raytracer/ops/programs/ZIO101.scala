package io.tuliplogic.raytracer.ops.programs

import io.tuliplogic.raytracer.ops.programs.Metrics.{Prometheus, TestMetrics}
import zio._
import zio.console.Console
import zio.internal.{Platform, PlatformLive}

import scala.{App => SApp}

/**
 * 
 * ray-tracer-zio - 18/11/2019
 * Created with ♥ in Amsterdam
 */
object ZIO101 extends SApp {
  val salutation = console.putStr("Zdravo, ")
  val city = console.putStrLn("Ljubljana!!!")

  val prg: ZIO[Console, Nothing, Unit] = salutation *> city
  salutation.flatMap(_ => city)
//  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = ???
  val defaultRuntme = new DefaultRuntime{}
  defaultRuntme.unsafeRun(prg)


  val provided: ZIO[Any, Nothing, Unit] = prg.provide(Console.Live)
  val emptyRuntime = new Runtime[Any] {
    override val environment: Any = ()
    override val platform: Platform = PlatformLive.Default
  }
  emptyRuntime.unsafeRun(provided)
//  emptyRuntime.unsafeRun(prg)

  val prg2: ZIO[Metrics with Console, Nothing, Unit] = for {
    _ <- console.putStrLn("Hello")
    _ <- Metrics.>.inc("salutation")
    _ <- console.putStrLn("BeeScala")
    _ <- Metrics.>.inc("subject")
  } yield ()

  emptyRuntime.unsafeRun(prg2.provide(
    new Prometheus with Console.Live
    )
  )

  defaultRuntme.unsafeRun(prg2.provideSome[Console]( c =>
    new Prometheus with Console {
      val console: Console.Service[Any] = c.console
    }))


  val test =  for {
    ref <- Ref.make(List[String]())
    _   <- prg2.provide(new Console.Live with Metrics {
             val metrics = TestMetrics(ref)
           })
    calls <- ref.get
    _     <- UIO.effectTotal(assert(calls == List("salutation", "subject")))
  } yield ()
  emptyRuntime.unsafeRun(test)
}

trait Metrics {
  val metrics: Metrics.Service[Any]
}
object Metrics {
  trait Service[R] {
    def inc(label: String): ZIO[R, Nothing, Unit]
  }

  object > extends Service[Metrics] {
    override def inc(label: String): ZIO[Metrics, Nothing, Unit] =
      ZIO.accessM(_.metrics.inc(label))
  }

  trait Prometheus extends Metrics {
    val metrics = new Metrics.Service[Any] {
      def inc(label: String): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal(())
    }
  }

  case class TestMetrics(incCalls: Ref[List[String]]) extends Metrics.Service[Any] {
    def inc(label: String): ZIO[Any, Nothing, Unit] =
      incCalls.update(xs => xs :+ label).unit
  }

}