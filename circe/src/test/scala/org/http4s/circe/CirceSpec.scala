package org.http4s
package circe

import io.circe._
import org.http4s.EntityEncoderSpec.writeToString
import org.http4s.Status.Ok
import org.http4s.headers.`Content-Type`
import org.http4s.jawn.JawnDecodeSupportSpec
import org.specs2.specification.core.Fragment

import java.nio.charset.StandardCharsets

// Originally based on ArgonautSpec
class CirceSpec extends JawnDecodeSupportSpec[Json] {
  testJsonDecoder(jsonDecoder)

  sealed case class Foo(bar: Int)
  val foo = Foo(42)
  // Beware of possible conflicting shapeless versions if using the circe-generic module
  // to derive these.
  implicit val FooDecoder = Decoder.instance(_.get("bar")(Decoder[Int]).map(Foo))
  implicit val FooEncoder = Encoder.instance[Foo](foo => Json.obj("bar" -> Encoder[Int].apply(foo.bar)))

  "json encoder" should {
    val json = Json.obj("test" -> Json.fromString("CirceSupport"))

    "have json content type" in {
      jsonEncoder.headers.get(`Content-Type`) must_== Some(`Content-Type`(MediaType.`application/json`))
    }

    "write compact JSON" in {
      writeToString(json) must_== ("""{"test":"CirceSupport"}""")
    }
  }

  "jsonEncoderOf" should {
    "have json content type" in {
      jsonEncoderOf[Foo].headers.get(`Content-Type`) must_== Some(`Content-Type`(MediaType.`application/json`))
    }

    "write compact JSON" in {
      writeToString(foo)(jsonEncoderOf[Foo]) must_== ("""{"bar":42}""")
    }
  }

  "json" should {
    "handle the optionality of asNumber" in {
      // From ArgonautSpec, which tests similar things:
      // TODO Urgh.  We need to make testing these smoother.
      // https://github.com/http4s/http4s/issues/157
      def getBody(body: EntityBody): Array[Byte] = body.runLog.run.reduce(_ ++ _).toArray
      val req = Request().withBody(Json.fromDoubleOrNull(157)).run
      val body = req.decode { json: Json => Response(Ok).withBody(json.asNumber.flatMap(_.toLong).getOrElse(0L).toString)}.run.body
      new String(getBody(body), StandardCharsets.UTF_8) must_== "157"
    }
  }

  "jsonOf" should {
    "decode JSON from a Circe decoder" in {
      val result = jsonOf[Foo].decode(Request().withBody(Json.obj("bar" -> Json.fromDoubleOrNull(42))).run, strict = true)
      result.run.run must be_\/-(Foo(42))
    }

    // https://github.com/http4s/http4s/issues/514
    Fragment.foreach(Seq("ärgerlich", """"ärgerlich"""")) { wort =>
      sealed case class Umlaut(wort: String)
      implicit val umlautDecoder = Decoder.instance(_.get("wort")(Decoder[String]).map(Umlaut))
      s"handle JSON with umlauts: $wort" >> {
        val json = Json.obj("wort" -> Json.fromString(wort))
        val result = jsonOf[Umlaut].decode(Request().withBody(json).run, strict = true)
        result.run.run must be_\/-(Umlaut(wort))
      }
    }
  }
}
