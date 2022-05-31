package org.virtuslab.ideprobe.log.legacy

import com.intellij.openapi.diagnostic.{ExceptionWithAttachments, IdeaLoggingEvent, RuntimeExceptionWithAttachments}
import com.intellij.util.ExceptionUtil
import org.apache.log4j._
import org.apache.log4j.spi.LoggingEvent
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.log.{IdeaLogParser, Message, MessageLog}
import org.virtuslab.ideprobe.protocol.IdeMessage
import org.virtuslab.ideprobe.handlers.App.ReflectionOps
import org.virtuslab.ideprobe.log.legacy.IdeaLogInterceptor.getMethod

import java.lang.reflect._
import scala.annotation.tailrec

object IdeaLogInterceptor extends IntelliJApi {

  @tailrec
  final def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
    try cl.getDeclaredMethod(name, parameters: _*)
    catch {
      case e: NoSuchMethodException =>
        if (cl.getSuperclass == null) throw e
        else getMethod(cl.getSuperclass, name, parameters: _*)
    }
  }

  def inject(): Unit = {
    val rootLogger = LogManager.getRootLogger
    val method = getMethod(rootLogger.getClass, "addAppender", classOf[Appender])
    method.invoke(rootLogger, new IdeaLogInterceptor)
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
    val rootLogger = LogManager.getRootLogger
    val appenders = rootLogger.getAllAppenders.asInstanceOf[java.util.Enumeration[Appender]]
    appenders.asScala.collect {
      case fileAppender: FileAppender =>
        val getImmediateFlush = getMethod(fileAppender.getClass,"getImmediateFlush")
        val setImmediateFlush = getMethod(fileAppender.getClass,"setImmediateFlush")
        val originalFlush = getImmediateFlush.invoke(fileAppender)
        setImmediateFlush.invoke(fileAppender, Boolean.box(true))
        rootLogger.info("Flush logs")
        setImmediateFlush.invoke(fileAppender, originalFlush)
    }
  }
}

class IdeaLogInterceptor extends AppenderSkeleton {

  def append(loggingEvent: LoggingEvent): Unit = {
    val getLevel = getMethod(loggingEvent.getClass,"getLevel")
    if (getLevel.invoke(loggingEvent).equals(Level.ERROR)) { // TODO: rever to `isGreaterOrEqual`
      messageFromIdeaLoggingEvent(loggingEvent, IdeMessage.Level.Error).foreach(MessageLog.add)
    } else if (getLevel.invoke(loggingEvent).equals(Level.WARN)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Warn)
      MessageLog.add(msg)
    } else if (getLevel.invoke(loggingEvent).equals(Level.INFO)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Info)
      MessageLog.add(msg)
    }
  }

  private def extractAnyMessage(loggingEvent: LoggingEvent, level: IdeMessage.Level) = {
    messageFromIdeaLoggingEvent(loggingEvent, level)
      .getOrElse(simpleMessage(loggingEvent, level))
  }

  private def messageFromIdeaLoggingEvent(loggingEvent: LoggingEvent, level: IdeMessage.Level): Option[Message] = {
    extractIdeaLoggingEvent(loggingEvent).map { ideaLoggingEvent =>
      val message = anyToString(ideaLoggingEvent)
      Message(message, Option(ideaLoggingEvent.getThrowable), level)
    }
  }

  // logic here roughly matches com.intellij.diagnostic.DialogAppender that is disabled in headless mode
  private def extractIdeaLoggingEvent(loggingEvent: LoggingEvent): Option[IdeaLoggingEvent] = {
    val getMessage = getMethod(loggingEvent.getClass,"getMessage")
    getMessage.invoke(loggingEvent) match {
      case e: IdeaLoggingEvent => Some(e)
      case otherMessage =>
        val getThrowableInformation = getMethod(loggingEvent.getClass,"getThrowableInformation")
        for {
          info <- Option(getThrowableInformation.invoke(loggingEvent))
          throwable <- Option(getMethod(info.getClass,"getThrowable").invoke(info))
        } yield {
          ExceptionUtil.getRootCause(throwable.asInstanceOf[Throwable]) match {
            case logEventEx if logEventEx.getClass.getName == "com.intellij.diagnostic.LogEventException" =>
              val getLogMessage = getMethod(logEventEx.getClass,"getLogMessage")
              getLogMessage.invoke(logEventEx).asInstanceOf[IdeaLoggingEvent]
            case _ =>
              val msg =
                ExceptionUtil.findCause(throwable.asInstanceOf[Throwable], classOf[ExceptionWithAttachments]) match {
                  case re: RuntimeExceptionWithAttachments =>
                    val getUserMessage = getMethod(re.getClass,"getUserMessage")
                    getUserMessage.invoke(re).asInstanceOf[String]
                  case _ => Option(otherMessage).fold("")(_.toString)
                }
              new IdeaLoggingEvent(msg, throwable.asInstanceOf[Throwable])
          }
        }
    }
  }

  private def simpleMessage(loggingEvent: LoggingEvent, level: IdeMessage.Level): Message = {
    val getMessage = getMethod(loggingEvent.getClass,"getMessage")
    Message(anyToString(getMessage.invoke(loggingEvent)), throwable = None, level)
  }

  private def anyToString(any: Any): Option[String] = {
    Option(any).map(_.toString).map(_.trim).filter(_.nonEmpty)
  }

  def close(): Unit = ()

  def requiresLayout(): Boolean = false
}
