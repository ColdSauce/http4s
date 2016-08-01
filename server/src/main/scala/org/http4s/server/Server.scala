package org.http4s
package server

import scalaz.concurrent.Task

import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

trait Server {
  def shutdown: Task[Unit]

  def shutdownNow(): Unit =
    shutdown.run

  @deprecated("Compose with the shutdown task instead.", "0.14")
  def onShutdown(f: => Unit): this.type

  def address: InetSocketAddress

  /**
   * Blocks until the server shuts down.
   */
  @deprecated("Use ServerApp instead.", "0.14")
  def awaitShutdown(): Unit = {
    val latch = new CountDownLatch(1)
    onShutdown(latch.countDown())
    latch.await()
  }
}


