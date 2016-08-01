package org.http4s
package headers

import org.http4s.parser.HttpHeaderParser
import org.http4s.util.{Renderer, Writer}

import java.time.Instant

object Date extends HeaderKey.Internal[Date] with HeaderKey.Singleton {
  override def parse(s: String): ParseResult[Date] =
    HttpHeaderParser.DATE(s)
}

final case class Date(date: Instant) extends Header.Parsed {
  def key = `Date`
  override def value = Renderer.renderString((date))
  override def renderValue(writer: Writer): writer.type = writer.append(value)
}

