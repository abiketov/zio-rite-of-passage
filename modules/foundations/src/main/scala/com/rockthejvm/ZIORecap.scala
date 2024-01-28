package com.rockthejvm

import zio.*

object ZIORecap extends ZIOAppDefault {

  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)

  val printMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))


  val anAttempt: Task[Int] = ZIO.attempt{
    println("Trying something")
    val string: String = null
    string.length
  }

  val catchError = for {
    s <- anAttempt.catchAll(e => ZIO.succeed(s"Returning some different value"))
    _ <- Console.printLine(s)
  } yield ()


  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)

  val interruptedFiber = for {
    fib <- delayedValue.onInterrupt(ZIO.succeed(println("I'm interrupted"))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield()

  val notInterruptedFiber = for {
    fib <- ZIO.uninterruptible(delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted")))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = notInterruptedFiber
}
