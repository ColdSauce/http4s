package org.http4s.util

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

object threads {
  final case class ThreadPriority(toInt: Int)
  case object ThreadPriority {
    val Min = ThreadPriority(Thread.MIN_PRIORITY)
    val Norm = ThreadPriority(Thread.NORM_PRIORITY)
    val Max = ThreadPriority(Thread.MAX_PRIORITY)
  }

  def threadFactory(
    name: Long => String = { l => s"http4s-$l" },
    daemon: Boolean = false,
    priority: ThreadPriority = ThreadPriority.Norm,
    uncaughtExceptionHandler: PartialFunction[(Thread, Throwable), Unit] = PartialFunction.empty,
    backingThreadFactory: ThreadFactory = Executors.defaultThreadFactory
  ): ThreadFactory =
    new ThreadFactory {
      val count = new AtomicLong(0)
      override def newThread(r: Runnable): Thread = {
        val thread = backingThreadFactory.newThread(r)
        thread.setName(name(count.getAndIncrement))
        thread.setDaemon(daemon)
        thread.setPriority(priority.toInt)
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
          override def uncaughtException(t: Thread, e: Throwable): Unit =
            (uncaughtExceptionHandler orElse fallthrough)((t, e))
          val fallthrough: PartialFunction[(Thread, Throwable), Unit] = {
            case (t: Thread, e: Throwable) =>
              Option(t.getThreadGroup)
                .getOrElse(Thread.getDefaultUncaughtExceptionHandler)
                .uncaughtException(t, e)
          }
        })
        thread
      }
    }

  /** Creates a thread pool marked with the DefaultExecutorService trait, so we know to shut it down. */
  private[http4s] def newDefaultFixedThreadPool(n: Int, threadFactory: ThreadFactory): ExecutorService =
    new ThreadPoolExecutor(n, n,
      0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue[Runnable],
      threadFactory)
}
