package org.virtuslab.ideprobe.log.legacy

import com.intellij.diagnostic.LogEventException
import com.intellij.openapi.diagnostic.{ExceptionWithAttachments, IdeaLoggingEvent, RuntimeExceptionWithAttachments}
import com.intellij.util.ExceptionUtil
import org.apache.log4j._
import org.apache.log4j.spi.LoggingEvent
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.log.{IdeaLogParser, Message, MessageLog}
import org.virtuslab.ideprobe.protocol.IdeMessage
import org.virtuslab.ideprobe.handlers.App.ReflectionOps

import java.lang.reflect._
import scala.annotation.tailrec

object IdeaLogInterceptor extends IntelliJApi {

  private val logger: Logger = LogManager.getLogger(classOf[IdeaLogInterceptor])

  @tailrec
  final def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
    try cl.getDeclaredMethod(name, parameters: _*)
    catch {
      case e: NoSuchMethodException =>
        if (cl.getSuperclass == null) throw e
        else getMethod(cl.getSuperclass, name, parameters: _*)
    }
  }

  @tailrec
  private def getMethods(methods: List[Method], cl: Class[_]): List[Method] = {
    val newMethods = cl.getDeclaredMethods.toList ++ methods
      if (cl.getSuperclass == null) methods
      else getMethods(newMethods, cl.getSuperclass)
  }

  def inject(): Unit = {
    println("> inject")
    logger.info("> inject")
    val rootLogger = LogManager.getRootLogger
    val method = getMethod(rootLogger.getClass, "addAppender", classOf[Appender])
    logger.error(s"logger methods:${getMethods(Nil, rootLogger.getClass)}, $method")
    method.invoke(rootLogger, new IdeaLogInterceptor)
    val errors = readInitialErrorsFromLogFile()
    errors.foreach(MessageLog.add)
    logger.info("< inject")
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
        val originalFlush = fileAppender.getImmediateFlush
        fileAppender.setImmediateFlush(true)
        rootLogger.info("Flush logs")
        fileAppender.setImmediateFlush(originalFlush)
    }
  }
}

class IdeaLogInterceptor extends AppenderSkeleton {

  def append(loggingEvent: LoggingEvent): Unit = {
    if (loggingEvent.getLevel.equals(Level.ERROR)) { // TODO: rever to `isGreaterOrEqual`
      messageFromIdeaLoggingEvent(loggingEvent, IdeMessage.Level.Error).foreach(MessageLog.add)
    } else if (loggingEvent.getLevel.equals(Level.WARN)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Warn)
      MessageLog.add(msg)
    } else if (loggingEvent.getLevel.equals(Level.INFO)) {
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
    loggingEvent.getMessage match {
      case e: IdeaLoggingEvent => Some(e)
      case otherMessage =>
        for {
          info <- Option(loggingEvent.getThrowableInformation)
          throwable <- Option(info.getThrowable)
        } yield {
          ExceptionUtil.getRootCause(throwable) match {
            case logEventEx: LogEventException => logEventEx.getLogMessage.asInstanceOf[IdeaLoggingEvent]
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

  private def simpleMessage(loggingEvent: LoggingEvent, level: IdeMessage.Level): Message = {
    Message(anyToString(loggingEvent.getMessage), throwable = None, level)
  }

  private def anyToString(any: Any): Option[String] = {
    Option(any).map(_.toString).map(_.trim).filter(_.nonEmpty)
  }

  def close(): Unit = ()

  def requiresLayout(): Boolean = false
}
