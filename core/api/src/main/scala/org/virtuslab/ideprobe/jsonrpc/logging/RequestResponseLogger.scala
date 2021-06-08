package org.virtuslab.ideprobe.jsonrpc.logging

import java.time.Instant
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor}
import scala.concurrent.duration.{Duration, SECONDS}

class RequestResponseLogger {

  private val ex = new ScheduledThreadPoolExecutor(1)
  private val flushTask: Runnable = () => flush()
  private var scheduledTask: Option[ScheduledFuture[_]] = None

  private val flushTimeout = Duration(1, SECONDS)

  private sealed trait LogEntry
  private case class Request(message: String) extends LogEntry
  private case class Response(message: String) extends LogEntry
  private case class RequestAndResponse(request: String, response: String) extends LogEntry

  private var buffer: List[(LogEntry, Instant)] = List.empty

  def logRequest(request: String): Unit = buffer.synchronized {
    scheduledTask.foreach(_.cancel(true))
    buffer match {
      case (head @ (RequestAndResponse(`request`, _), _)) :: tail =>
        buffer = (Request(request), Instant.now()) :: head :: tail
      case _ =>
        flush()
        buffer = (Request(request), Instant.now()) :: Nil
    }
    scheduledTask = Option(ex.schedule(flushTask, flushTimeout.length, flushTimeout.unit))
  }

  def logResponse(response: String): Unit = buffer.synchronized {
    scheduledTask.foreach(_.cancel(true))
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
    scheduledTask = Option(ex.schedule(flushTask, flushTimeout.length, flushTimeout.unit))
  }

  private def flush(): Unit = buffer.synchronized {
    val now = Instant.now()
    val firstMessageTimestamp = buffer.reverse.headOption.map(_._2).getOrElse(now)
    collectCountingSubsequent(buffer.map(_._1).reverse).foreach {
      case (RequestAndResponse(request, response), 1) =>
        println(request)
        println(response)
      case (RequestAndResponse(request, response), count) =>
        println(s"""Repeated $count times over ${java.time.temporal.ChronoUnit.SECONDS.between(firstMessageTimestamp, now)} seconds: {
                 |  $request
                 |  $response
                 |}""".stripMargin)
      case (Request(request), _) => //Here and below the count will always be 1 - see the buffering mechanism.
        println(request)
      case (Response(response), _) =>
        println(response)
    }
    buffer = Nil
  }

  private def collectCountingSubsequent[X](xs: List[X]): List[(X, Int)] =
    xs.foldLeft(List.empty[(X, Int)]) { (acc, elem) =>
      (acc.reverse match {
        case (`elem`, count) :: tail => (elem, count + 1) :: tail
        case other => (elem, 1) :: other
      }).reverse
    }
}
