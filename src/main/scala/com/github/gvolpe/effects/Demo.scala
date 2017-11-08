package com.github.gvolpe.effects

import cats.effect._

import scala.concurrent.{ExecutionContext, Future}

// TODO: Include Monix when M2 is released
object Demo extends EffectDemo with AsyncDemo with SyncDemo with LiftIODemo with App {

//  delay[IO].unsafeRunSync()
//  suspend[IO].unsafeRunSync()

  val io: IO[String] = IO("Hello World!")

  val result: Future[String] = liftIO[Future, String](io)

  println(result)

//  runAsync(io).unsafeRunSync()

//  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
//
//  val result: String = async[IO].unsafeRunSync()
//  println(result)

}

trait EffectDemo {

  def runAsync[F[_] : Effect, A](task: F[A]): IO[Unit] =
    Effect[F].runAsync(task) {
      case Right(value) => IO(println(value))
      case Left(error)  => IO.raiseError(error)
    }

}

trait AsyncDemo {

  private val futureOfString = Future.successful("Hi, I come from the Future!")

  // Useful for integration with callback-based APIs
  def async[F[_] : Async](implicit ec: ExecutionContext): F[String] =
    Async[F].async { cb =>
      import scala.util.{Failure, Success}

      futureOfString.onComplete {
        case Success(value) => cb(Right(value))
        case Failure(error) => cb(Left(error))
      }
    }

}

trait SyncDemo {

  def delay[F[_] : Sync]: F[Unit] = Sync[F].delay {
    println("delay(A) === suspend(F[A])")
  }

  def suspend[F[_] : Sync]: F[Unit] = Sync[F].suspend {
    Sync[F].pure(println("suspend(F[A])"))
  }

}

trait LiftIODemo {

  implicit def listLiftIO: LiftIO[List] =
    new LiftIO[List] {
      override def liftIO[A](ioa: IO[A]): List[A] = {
        ioa.attempt.unsafeRunSync.fold(_ => List.empty[A], List(_))
      }
    }

  implicit def futureLiftIO: LiftIO[Future] =
    new LiftIO[Future] {
      override def liftIO[A](ioa: IO[A]): Future[A] =
        ioa.unsafeToFuture()
    }

  def liftIO[F[_] : LiftIO, A](ioa: IO[A]): F[A] =
    LiftIO[F].liftIO(ioa)
}
