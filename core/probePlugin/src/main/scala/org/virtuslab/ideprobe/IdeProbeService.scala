package org.virtuslab.ideprobe

import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext

import com.intellij.openapi.components.ServiceManager

final class IdeProbeService {
  private implicit val executionContext: ExecutionContext = IdeProbeService.executionContext

  def start(): Unit = {
    sys.env.get("IDEPROBE_DRIVER_PORT") match {
      case Some(port) =>
        val socket = new Socket("localhost", port.toInt)
        println(s"Probe connected to ${socket.getInetAddress}:${socket.getPort}")
        Probe.start(socket)
      case None =>
        val error = "Driver port not specified. Use environment variable: [IDEPROBE_DRIVER_PORT]"
        new IllegalStateException(error)
    }
  }
}

object IdeProbeService {
  val executionContext: ExecutionContext = {
    val id = new AtomicInteger(0)
    def threadId = s"ideprobe-worker-${id.incrementAndGet()}"
    val executor = Executors.newCachedThreadPool(action => new Thread(action, threadId))
    ExecutionContext.fromExecutor(executor)
  }

  def apply(): IdeProbeService = {
    ServiceManager.getService(classOf[IdeProbeService])
  }
}
