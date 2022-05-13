package org.virtuslab.ideprobe.protocol

import java.nio.file.Path
import pureconfig.ConfigConvert

import scala.collection.mutable

case class HighlightInfo(
    origin: Path,
    line: Int,
    offsetStart: Int,
    offsetEnd: Int,
    severity: HighlightInfo.Severity,
    description: String
)

object HighlightInfo {
  case class Severity(name: String, value: Int) extends Ordered[Severity] {
    override def compare(that: Severity): Int = this.value.compare(that.value)
  }

  object Severity {
    private val values = mutable.Map[String, Severity]()

    val Information = create("INFORMATION", 10)
    val GenericServerErrorOrWarning = create("GENERIC SERVER ERROR OR WARNING", 100)
    val Info = create("INFO", 200)
    val WeakWarning = create("WEAK WARNING", 200)
    val Warning = create("WARNING", 300)
    val Error = create("ERROR", 400)

    def from(name: String, value: Int): Severity = {
      values.getOrElse(name, create(name, value))
    }

    private def create(name: String, value: Int): Severity = {
      val severity = Severity(name, value)
      values.put(name, severity)
      severity
    }
  }

}
