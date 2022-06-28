package org.virtuslab.ideprobe.reporting

import org.virtuslab.ideprobe.config.CheckConfig
import org.virtuslab.ideprobe.protocol.IdeMessage

object ErrorValidator {
  def apply(config: CheckConfig, errors: Seq[IdeMessage]): Option[Exception] = {
    if (errors.isEmpty) None
    else {
      println(toString(errors))
      if (!config.errors) None
      else Some(new Exception(toString(errors)))
    }
  }

  private def toString(errors: Seq[IdeMessage]): String = {
    val sb = new StringBuilder()

    errors.groupBy(_.pluginId.getOrElse("IDEA")).foreach { case (group, errors) =>
      sb.append(s"Errors caused by $group >>>\n")
      errors.foreach(msg => sb.append(s"\t${msg.content}\n"))
      sb.append("<<<")
    }
    sb.toString()
  }
}
