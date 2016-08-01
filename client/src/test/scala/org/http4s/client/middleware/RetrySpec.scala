package org.http4s
package client
package middleware

import scalaz.concurrent.Task

import org.http4s.Method._
import org.http4s.Status._

import scala.concurrent.duration._
import scala.language.postfixOps

class RetrySpec extends Http4sSpec {

  val route = HttpService {
    case r if r.method == GET && r.pathInfo == "/ok" => Response(Ok).withBody("hello")
    case r if r.method == GET && r.pathInfo == "/boom" => Task.now(Response(BadRequest))
    case r => sys.error("Path not found: " + r.pathInfo)
  }

  val defaultClient = MockClient(route)

  "Retry Client" should {
    "Retry bad requests" in {
      val max = 2
      var attemptsCounter = 1
      val policy = (attempts: Int) => {
        if (attempts >= max) None
        else {
          attemptsCounter = attemptsCounter + 1
          Some(10.milliseconds)
        }
      }
      val client = Retry(policy)(defaultClient)
      val resp = client.expect[String](uri("http://localhost/boom")).run
      attemptsCounter must_== 2
    }

    "Not retry successful responses" in {
      val max = 2
      var attempts = 1
      val policy = (attmpts: Int) => {
        if (attempts >= max) None
        else {
          attempts = attempts + 1
          Some(10.milliseconds)
        }
      }
      val client = Retry(policy)(defaultClient)
      val resp = client.expect[String](uri("http://localhost/ok")).run
      attempts must_==1
    }
  }
}
