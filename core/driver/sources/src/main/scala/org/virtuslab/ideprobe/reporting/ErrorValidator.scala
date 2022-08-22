package org.virtuslab.ideprobe.reporting

import scala.collection.mutable

import org.virtuslab.ideprobe.config.CheckConfig
import org.virtuslab.ideprobe.protocol.IdeMessage

object ErrorValidator {
  def apply(config: CheckConfig, errors: Seq[IdeMessage]): Option[Exception] = {
    val filteredErrors = errors
      .filter(ideMessageMatchesMessagesFromConfig(_, config.errors.includeMessages))
      .filterNot(ideMessageMatchesMessagesFromConfig(_, config.errors.excludeMessages))
    if (filteredErrors.isEmpty) None
    else {
      println(toString(filteredErrors))
      if (!config.errors.enabled) None
      else {
        Some(new Exception(toString(filteredErrors)))
      }
    }
  }

  private def ideMessageMatchesMessagesFromConfig(ideMessage: IdeMessage, messagesFromConfig: Seq[String]): Boolean =
    messagesFromConfig.exists(configMessage => trimMultiLine(ideMessage.content).contains(trimMultiLine(configMessage)))

  private def trimMultiLine(s: String): String = s.split('\n').map(_.trim).mkString("\n")

  private def toString(errors: Seq[IdeMessage]): String = {
    val sb = new mutable.StringBuilder()

    errors.groupBy(_.pluginId.getOrElse("IDEA")).foreach { case (group, errors) =>
      sb.append(s"Errors caused by $group >>>\n")
      errors.foreach(msg => sb.append(s"\t${msg.content}\n"))
      sb.append("<<<")
    }
    sb.toString()
  }
}
