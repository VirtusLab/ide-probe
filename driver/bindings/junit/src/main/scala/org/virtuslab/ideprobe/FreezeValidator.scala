package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.ide.intellij.CheckConfig
import org.virtuslab.ideprobe.protocol.Freeze

object FreezeValidator {
  def apply(config: CheckConfig, freezes: Seq[Freeze]): Option[Exception] = {
    if (freezes.isEmpty) None
    else {
      println(toString(freezes))
      if (!config.freezes) None
      else Some(new Exception(toString(freezes)))
    }
  }

  private def toString(freezes: Seq[Freeze]): String = {
    val sb = new StringBuilder("UI freezes during test >>>\n")
    freezes.sortBy(_.duration).map(toString).foreach(sb.append)
    sb.append("<<<").toString()
  }

  private def toString(freeze: Freeze): String = {
    val title = freeze.duration match {
      case Some(value) => s"UI froze for ${value.toSeconds}s"
      case None        => "UI froze"
    }

    freeze.edtStackTrace.mkString(s"$title\n", "\t\n", "\n")
  }
}
