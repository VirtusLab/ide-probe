package org.virtuslab.ideprobe.protocol

import java.nio.file.Path
import pureconfig.ConfigConvert

case class HighlightInfo(
    origin: Path,
    line: Int,
    offsetStart: Int,
    offsetEnd: Int,
    severity: HighlightInfo.Severity.Value,
    description: String
)

object HighlightInfo {
  object Severity extends Enumeration {
    val Information = Value("INFORMATION")
    val GenericServerErrorOrWarning = Value("GENERIC SERVER ERROR OR WARNING")
    val Info = Value("INFO")
    val WeakWarning = Value("WEAK WARNING")
    val Warning = Value("WARNING")
    val Error = Value("ERROR")
  }

  implicit val severityFormat: ConfigConvert[Severity.Value] = {
    ConfigConvert[String].xmap(Severity.withName, _.toString)
  }
}
