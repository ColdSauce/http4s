package org.http4s
package headers

import compat._

import org.http4s.parser.HttpHeaderParser
import cats.data.NonEmptyList

object `Accept-Encoding` extends HeaderKey.Internal[`Accept-Encoding`] with HeaderKey.Recurring {
  override def parse(s: String): ParseResult[`Accept-Encoding`] =
    HttpHeaderParser.ACCEPT_ENCODING(s)
}

final case class `Accept-Encoding`(values: NonEmptyList[ContentCoding]) extends Header.RecurringRenderable {
  def key = `Accept-Encoding`
  type Value = ContentCoding
  def preferred: ContentCoding = values.tail.fold(values.head)((a, b) => if (a.qValue >= b.qValue) a else b)
  def satisfiedBy(coding: ContentCoding): Boolean = values.exists(_.satisfiedBy(coding))
}
