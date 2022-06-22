package org.virtuslab.ideprobe.reporting

import org.virtuslab.ideprobe.config.CheckConfig
import org.virtuslab.ideprobe.protocol.IdeMessage

object ErrorValidator {
  def apply(config: CheckConfig, errors: Seq[IdeMessage]): Option[Exception] = {
    println("CHECK CONFIG")
    println(config)
    config.errorMessages.foreach(s => println(s))
    val seq: Seq[String] = Seq("<html>IntelliJ IDEA 2022.1.3 available</html>")
    errors.foreach(s => println(s.content))
    val filtered = errors.filter(p=> !seq.contains(p.content))
    println("FILTERED")
    filtered.foreach(s => println(s.content))
    if (filtered.isEmpty) None
    else {
      println(toString(filtered))
      if (!config.errors) None
      else {
        Some(new Exception(toString(filtered)))
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
