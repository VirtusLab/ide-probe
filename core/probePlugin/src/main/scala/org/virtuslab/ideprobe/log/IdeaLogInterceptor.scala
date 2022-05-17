package org.virtuslab.ideprobe.log

import com.intellij.openapi.diagnostic.{ExceptionWithAttachments, IdeaLoggingEvent, RollingFileHandler, RuntimeExceptionWithAttachments}
import com.intellij.util.ExceptionUtil
import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.IdeMessage

import java.util.logging.{Handler, Level, LogManager, LogRecord}

object IdeaLogInterceptor extends IntelliJApi {

  def inject(): Unit = {
    LogManager.getLogManager.getLogger("").addHandler(new IdeaLogInterceptor)

    val errors = readInitialErrorsFromLogFile()
    errors.foreach(MessageLog.add)
  }

  private def readInitialErrorsFromLogFile(): Seq[Message] = {
    flushFileLogger()
    val initialLog = ideaLogPath.content()
    val errors = IdeaLogParser.extractErrors(initialLog, fromLastStart = true)
    errors.map(err => Message(content = Some(err), throwable = None, IdeMessage.Level.Error))
  }

  private def ideaLogPath = {
    logsPath.resolve("idea.log")
  }

  private def flushFileLogger(): Unit = {
    val rootLogger = LogManager.getLogManager.getLogger("")
    val appenders = rootLogger.getHandlers.asInstanceOf[java.util.Enumeration[Handler]]
    appenders.asScala.collect {
      case fileAppender: RollingFileHandler =>
        fileAppender.flush()
        rootLogger.info("Flush logs")
    }
  }
}

class IdeaLogInterceptor extends Handler {

  override def publish(loggingEvent: LogRecord): Unit = {
    if (loggingEvent.getLevel.equals(Level.SEVERE)) {
      messageFromIdeaLoggingEvent(loggingEvent, IdeMessage.Level.Error).foreach(MessageLog.add)
    } else if (loggingEvent.getLevel.equals(Level.WARNING)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Warn)
      MessageLog.add(msg)
    } else if (loggingEvent.getLevel.equals(Level.INFO)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Info)
      MessageLog.add(msg)
    }
  }

  private def extractAnyMessage(loggingEvent: LogRecord, level: IdeMessage.Level) = {
    messageFromIdeaLoggingEvent(loggingEvent, level)
      .getOrElse(simpleMessage(loggingEvent, level))
  }

  private def messageFromIdeaLoggingEvent(loggingEvent: LogRecord, level: IdeMessage.Level): Option[Message] = {
    extractIdeaLoggingEvent(loggingEvent).map { ideaLoggingEvent =>
      val message = anyToString(ideaLoggingEvent)
      Message(message, Option(ideaLoggingEvent.getThrowable), level)
    }
  }

  // logic here roughly matches com.intellij.diagnostic.DialogAppender that is disabled in headless mode
  private def extractIdeaLoggingEvent(loggingEvent: LogRecord): Option[IdeaLoggingEvent] = {
    val parameters: Seq[AnyRef] = loggingEvent.getParameters
    parameters match {
      case (a: IdeaLoggingEvent) :: _ => Some(a)
      case otherMessage =>
        for {
          throwable <- Option(loggingEvent.getThrown)
        } yield {
          ExceptionUtil.getRootCause(throwable) match {
            case _ =>
              val msg =
                ExceptionUtil.findCause(throwable, classOf[ExceptionWithAttachments]) match {
                  case re: RuntimeExceptionWithAttachments =>
                    re.getUserMessage
                  case _ => Option(otherMessage).fold("")(_.toString)
                }
              new IdeaLoggingEvent(msg, throwable)
          }
        }
    }
  }

  private def simpleMessage(loggingEvent: LogRecord, level: IdeMessage.Level): Message = {
    Message(anyToString(loggingEvent.getMessage), throwable = None, level)
  }

  private def anyToString(any: Any): Option[String] = {
    Option(any).map(_.toString).map(_.trim).filter(_.nonEmpty)
  }



  override def close(): Unit = ()

  override def flush(): Unit = ()
}
