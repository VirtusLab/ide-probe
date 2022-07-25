package org.virtuslab.ideprobe

import java.net.{HttpURLConnection, URL}

import scala.concurrent.duration.DurationInt

package object download {

  implicit class Any2Option[T <: Any](any: T) {
    def lift2Option: Option[T] = Option(any)
  }

  def withConnection[V](url: URL)(f: => HttpURLConnection => V): V = {
    var connection: HttpURLConnection = null
    try {
      connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(5.seconds.toMillis.toInt)
      connection.setReadTimeout(30.seconds.toMillis.toInt)
      f(connection)
    } finally {
      try {
        if (connection != null) connection.disconnect()
      } catch {
        case e: Exception =>
          println(s"Failed to close connection $url: ${e.getMessage}")
      }
    }
  }

  // noinspection MutatorLikeMethodIsParameterless
  implicit class StringUtils(str: String) {
    def removeSpaces: String = str.replaceAll("\\s+", "")
    def escapeSpaces: String = str.replaceAll("\\s", "\\ ")
    def xmlQuote: String = s"&quot;$str&quot;"
    def isValidFileName: Boolean =
      str.matches(
        "\\A(?!(?:COM[0-9]|CON|LPT[0-9]|NUL|PRN|AUX|com[0-9]|con|lpt[0-9]|nul|prn|aux)|[\\s\\.])[^\\\\\\/:*\"?<>|]{1,254}\\z"
      )
  }

}
