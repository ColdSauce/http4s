package org.http4s

import java.nio.CharBuffer
import java.nio.charset.CharsetDecoder
import java.util.concurrent.{ExecutorService, Executors}

import scodec.bits.ByteVector

import scalaz.{Monad, Functor, State}
import fs2.Task
import fs2.Process1
import fs2.Stream._
/*
import scalaz.stream.io.bufferedChannel
*/
import scalaz.std.option.none

package object util {
  /** Temporary.  Contribute back to scalaz-stream. */
  def decode(charset: Charset): Process1[ByteVector, String] = suspend {
    val decoder = charset.nioCharset.newDecoder
    var carryOver = ByteVector.empty

    def push(chunk: ByteVector, eof: Boolean) = {
      val in = carryOver ++ chunk
      val byteBuffer = in.toByteBuffer
      val charBuffer = CharBuffer.allocate(in.size.toInt + 1)
      decoder.decode(byteBuffer, charBuffer, eof)
      if (eof)
        decoder.flush(charBuffer)
      else
        carryOver = ByteVector.view(byteBuffer.slice)
      charBuffer.flip().toString
    }

    // A ByteVector can now be longer than Int.MaxValue, but the CharBuffer
    // above cannot.  We need to split enormous chunks just in case.
    def breakBigChunks(): Process1[ByteVector, ByteVector] =
      receive1[ByteVector, ByteVector] { chunk =>
        def loop(chunk: ByteVector): Process1[ByteVector, ByteVector] =
          chunk.splitAt(Int.MaxValue - 1) match {
            case (bv, ByteVector.empty) =>
              emit(bv) ++ breakBigChunks()
            case (bv, tail) =>
              emit(bv) ++ loop(tail)
          }
        loop(chunk)
      }

    def go(): Process1[ByteVector, String] = receive1[ByteVector, String] { chunk =>
      val s = push(chunk, false)
      val sChunk = if (s.nonEmpty) emit(s) else empty
      sChunk ++ go()
    }

    def flush() = {
      val s = push(ByteVector.empty, true)
      if (s.nonEmpty) emit(s) else empty
    }

    breakBigChunks() pipe go() onComplete flush()
  }

  /** Constructs an assertion error with a reference back to our issue tracker. Use only with head hung low. */
  def bug(message: String): AssertionError =
    new AssertionError(s"This is a bug. Please report to https://github.com/http4s/http4s/issues: ${message}")

  implicit val TaskMonad: Monad[Task] = new Monad[Task] {
    override def point[A](a: => A): Task[A] =
      Task.delay(a)

    override def bind[A, B](fa: Task[A])(f: (A) => Task[B]): Task[B] =
      fa.flatMap(f)
  }

  val DefaultExecutorService: ExecutorService =
    Executors.newFixedThreadPool(
      Runtime.getRuntime.availableProcessors,
      threads.threadFactory(i => s"http4s-$i"))
}
