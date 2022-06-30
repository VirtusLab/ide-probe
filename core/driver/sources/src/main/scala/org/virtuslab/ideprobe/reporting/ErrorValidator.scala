package org.virtuslab.ideprobe.reporting

import org.virtuslab.ideprobe.config.CheckConfig
import org.virtuslab.ideprobe.protocol.IdeMessage

object ErrorValidator {
  def apply(config: CheckConfig, errors: Seq[IdeMessage]): Option[Exception] = {
    val filteredErrors = errors
      .filter(error => config.errors.includeMessages.exists(error.content.matches))
      .filterNot(error => config.errors.excludeMessages.exists(error.content.matches))
    if (filteredErrors.isEmpty) None
    else {
      println(toString(filteredErrors))
      if (!config.errors.enabled) None
      else {
        Some(new Exception(toString(filteredErrors)))
      }
    }
  }

  private def toString(errors: Seq[IdeMessage]): String = {
    val sb = new StringBuilder()

    errors.groupBy(_.pluginId.getOrElse("IDEA")).foreach {
      case (group, errors) =>
        sb.append(s"Errors caused by $group >>>\n")
        errors.foreach(msg => sb.append(s"\t${msg.content}\n"))
        sb.append("<<<")
    }
    sb.toString()
  }
}
