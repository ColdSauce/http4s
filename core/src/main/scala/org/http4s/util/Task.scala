package org.http4s
package util

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Success, Failure}

trait TaskFunctions {
  def unsafeTaskToFuture[A](task: Task[A]): Future[A] = {
    val p = Promise[A]()
    task.runAsync {
      case \/-(a) => p.success(a)
      case -\/(t) => p.failure(t)
    }
    p.future
  }

  def futureToTask[A](f: => Future[A])(implicit ec: ExecutionContext): Task[A] = {
    Task.async { cb =>
      f.onComplete {
        case Success(a) => cb(a.right)
        case Failure(t) => cb(t.left)
      }
    }
  }
}

object task extends TaskFunctions
