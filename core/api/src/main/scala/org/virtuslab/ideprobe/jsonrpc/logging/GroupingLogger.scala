package org.virtuslab.ideprobe.jsonrpc.logging

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Try

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.jsonrpc.JsonRpc

case class LoggingConfig(
    groupingTimeWindow: FiniteDuration = 1.second,
    allowList: Seq[String] = Seq(),
    blockList: Seq[String] = Seq(
      "config/set",
      "trustedPaths/add",
      "freezes",
      "messages",
      "systemProperties",
      "pid",
      "plugins",
      "backgroundTasks"
    ).map(endpoint => s"request\\[$endpoint\\]: ")
)

object LoggingConfig extends ConfigFormat {
  implicit val format: ConfigConvert[LoggingConfig] = deriveConvert[LoggingConfig]
}

class GroupingLogger(config: LoggingConfig) extends ProbeCommunicationLogger {

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val flushTask: Runnable = () => flush()
  private var scheduledTask: Option[ScheduledFuture[_]] = None

  private val flushTimeout = config.groupingTimeWindow

  private sealed trait LogEntry
  private case class Request(message: String) extends LogEntry
  private case class Response(message: String) extends LogEntry
  private case class RequestAndResponse(request: String, response: String) extends LogEntry

  private var buffer: List[(LogEntry, Instant)] = Nil
  private var skipResponse = false

  override def close(): Unit = {
    flush()
    executor.shutdown()
  }

  def logRequest(name: String, param: String): Unit = synchronized {
    val request = s"request[$name]: ${decode(param)}"
    cancelScheduledFlush()
    buffer match {
      case (head @ (RequestAndResponse(`request`, _), _)) :: tail =>
        buffer = (Request(request), Instant.now()) :: head :: tail
      case _ =>
        flush()
        buffer = (Request(request), Instant.now()) :: Nil
    }
    scheduleFlush()
  }

  def logResponse(value: String): Unit = synchronized {
    val response = s"response: ${decode(value)}"
    cancelScheduledFlush()
    buffer match {
      case (Request(request), timestamp) :: Nil =>
        buffer = (RequestAndResponse(request, response), timestamp) :: Nil
      case (Request(ultimate), timestamp) :: (penultimate @ (RequestAndResponse(_, `response`), _)) :: tail =>
        buffer = (RequestAndResponse(ultimate, response), timestamp) :: penultimate :: tail
      case (Request(request), timestamp) :: tail =>
        buffer = tail
        flush()
        buffer = (RequestAndResponse(request, response), timestamp) :: Nil
      case _ =>
        buffer = (Response(response), Instant.now()) :: buffer
        flush()
    }
    scheduleFlush()
  }

  private def scheduleFlush(): Unit = {
    scheduledTask = Some(executor.schedule(flushTask, flushTimeout.length, flushTimeout.unit))
  }

  private def cancelScheduledFlush(): Unit = {
    scheduledTask.foreach(_.cancel(true))
  }

  private def flush(): Unit = synchronized {
    if (buffer.nonEmpty) {
      val now = Instant.now()
      val firstMessageTimestamp = buffer.last._2
      val grouped = collectCountingSubsequent(buffer.map(_._1).reverse)
      grouped.foreach {
        case (RequestAndResponse(request, response), 1) =>
          logFiltered(s"$request\n$response")
        case (RequestAndResponse(request, response), count) =>
          val durationDescr = formatSeconds(ChronoUnit.SECONDS.between(firstMessageTimestamp, now))
          logFiltered(s"""Repeated $count times $durationDescr: {
                     |  $request
                     |  $response
                     |}""".stripMargin)
        case (Request(request), _) => // Here and below the count will always be 1 - see the buffering mechanism.
          if (accept(request)) {
            log(request)
          } else {
            skipResponse = true
          }
        case (Response(response), _) =>
          if (!skipResponse) {
            logFiltered(response)
          }
          skipResponse = false
      }
      buffer = Nil
    }
  }

  private def accept(message: String): Boolean = {
    if (config.allowList.nonEmpty) {
      config.allowList.exists(pattern => testRegex(message, pattern))
    } else {
      !config.blockList.exists(pattern => testRegex(message, pattern))
    }
  }

  private def testRegex(message: String, pattern: String) = {
    pattern.r.pattern.asPredicate.test(message)
  }

  private def logFiltered(message: String): Unit = {
    if (accept(message)) log(message)
  }

  private def log(message: String): Unit = {
    println(message)
  }

  private def formatSeconds(secondsPassed: Long): String = secondsPassed match {
    case 0L => "in less than one second"
    case 1L => "over one second"
    case n  => s"over $n seconds"
  }

  private def collectCountingSubsequent[A](xs: List[A]): List[(A, Int)] = {
    @tailrec
    def collectCountingSubsequentRecursively(xs: List[A], collected: List[(A, Int)]): List[(A, Int)] = {
      xs match {
        case Nil => collected
        case head :: _ =>
          val (repeated, rest) = xs.span(_ == head)
          collectCountingSubsequentRecursively(rest, (head, repeated.length) :: collected)
      }
    }
    collectCountingSubsequentRecursively(xs, Nil)
  }

  private def decode(json: String): String = {
    Try {
      val dataAsAny = JsonRpc.gson.fromJson(json, classOf[util.Map[String, Any]]).get("data")
      if (dataAsAny == null) "()" else JsonRpc.gson.toJson(dataAsAny)
    }.getOrElse(json)
  }
}
