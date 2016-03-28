package org.http4s
package multipart

import java.io.{ File, FileInputStream, InputStream }
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.util.Random

import org.http4s._
import org.http4s.EntityEncoder._
import org.http4s.MediaType._
import org.http4s.headers._
import org.http4s.util.CaseInsensitiveString
import org.http4s.util.string._
import scalaz.std.string._
import scalaz.stream.Process.{ constant, emit }
import scalaz.stream.io.chunkR
import scalaz.stream.text.utf8Encode
import scodec.bits.{ BitVector, ByteVector }

final case class Part(headers: Headers, body: EntityBody) {
  def name: Option[CaseInsensitiveString] = headers.get(`Content-Disposition`).map(_.name)
}

object Part {
  val empty: Part =
    Part(Headers.empty, EmptyBody)

  def formData(name: String, value: String, headers: Header*): Part =
    Part(`Content-Disposition`("form-data", Map("name" -> name)) +: headers, emit(value) |> utf8Encode)

  def fileData(name: String, file: File, headers: Header*): Part =
    fileData(name, file.getName, new FileInputStream(file), headers:_*)

  def fileData(name: String, resource: URL, headers: Header*): Part = {
    fileData(name, resource.getPath.split("/").last, resource.openStream(), headers:_*)

  private def fileData(name: String, filename: String, in: => InputStream, headers: Header*): Part = {
    // TODO Reading the whole file into one chunk is abominable
    val body = BitVector.fromInputStream(in).toByteVector
    Part(`Content-Disposition`("form-data", Map("name" -> name, "filename" -> filename)) +:
           Header("Content-Transfer-Encoding", "binary") +:
           headers,
         constant(8192).toSource through chunkR(in))
  }
}

final case class Multipart(parts: Vector[Part], boundary: Boundary = Boundary.create) {
  def headers = Headers(`Content-Type`(MediaType.multipart("form-data", Some(boundary.value))))
}

case class Boundary(value: String) extends AnyVal {
  def toByteVector: ByteVector =
    ByteVector.view(value.getBytes(StandardCharsets.UTF_8))
}

object Boundary {
  val CRLF = "\r\n"
  
  private val DIGIT = ('0' to '9').toList
  private val ALPHA = ('a' to 'z').toList ++ ('A' to 'Z').toList
  private val OTHER = """'()+_,-./:=? """.toSeq 
  private val CHARS = (DIGIT ++ ALPHA ++ OTHER).toList
  private val nchars = CHARS.length
  private val rand = new Random()

  private def nextChar = CHARS(rand.nextInt(nchars - 1))
  private def stream: Stream[Char] = Stream continually (nextChar)
  //Don't use filterNot it works for 2.11.4 and nothing else, it will hang.
  private def endChar: Char = stream.filter(_ != ' ').headOption.getOrElse('X')
  private def value(l: Int): String = (stream take l).mkString

  def create = Boundary(value(40) + endChar)
}

