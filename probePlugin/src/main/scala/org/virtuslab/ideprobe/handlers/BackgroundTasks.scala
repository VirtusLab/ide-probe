package org.virtuslab.ideprobe.handlers

import java.lang.reflect.Method
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.AwaitIdleParams
import scala.annotation.tailrec
import scala.concurrent.duration._
import pureconfig.generic.auto._
import org.virtuslab.ideprobe.jsonrpc.PayloadJsonFormat._

object BackgroundTasks extends IntelliJApi {
  def withAwaitNone[A](block: => A): A = {
    val result = block
    awaitNone()
    result
  }

  def awaitNone(): Unit = {
    val config = ProbeConfig.get()
      .get[AwaitIdleParams]("probe.endpoints.awaitIdle")
      .getOrElse(AwaitIdleParams.Default)

    awaitNone(config)
  }

  @tailrec
  def awaitNone(params: AwaitIdleParams): Unit = {
    if (params.active) {
      sleep(params.initialWait)
      val tasks = currentBackgroundTasks()
      if (tasks.nonEmpty) {
        log.warn(s"Waiting for completion of $tasks")
        awaitNone(params)
      } else {
        if (newTaskAppeared(within = params.newTaskWait, probeFrequency = params.checkFrequency)) {
          awaitNone(params)
        }
      }
    }
  }

  @tailrec
  private def newTaskAppeared(within: FiniteDuration, probeFrequency: FiniteDuration): Boolean = {
    if (within <= 0.millis) {
      false
    } else {
      sleep(probeFrequency)
      val tasks = currentBackgroundTasks()
      if (tasks.nonEmpty) {
        true
      } else {
        newTaskAppeared(within - probeFrequency, probeFrequency)
      }
    }
  }

  def currentBackgroundTasks(): Seq[ProgressIndicator] = {
    val progressManager = ProgressManager.getInstance
    getCurrentIndicatorsMethod.invoke(progressManager).asInstanceOf[java.util.List[ProgressIndicator]].asScala.toSeq
  }

  def currentBackgroundTaskNames(): Seq[String] = {
    currentBackgroundTasks().map(_.toString).filter(_ != null)
  }

  private lazy val getCurrentIndicatorsMethod = {
    val method = getMethod(ProgressManager.getInstance.getClass, "getCurrentIndicators")
    method.setAccessible(true)
    method
  }

  @tailrec private def getMethod(cls: Class[_], name: String): Method = {
    try cls.getDeclaredMethod(name)
    catch {
      case e: NoSuchMethodException =>
        if (cls.getSuperclass != null) {
          getMethod(cls.getSuperclass, name)
        } else throw e
    }
  }

  private def sleep(duration: FiniteDuration): Unit = {
    Thread.sleep(duration.toMillis)
  }
}
