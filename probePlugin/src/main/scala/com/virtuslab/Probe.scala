package com.virtuslab

import java.net.Socket
import com.virtuslab.ideprobe.Close
import com.virtuslab.ideprobe.jsonrpc.JsonRpc
import com.virtuslab.ideprobe.jsonrpc.JsonRpcConnection
import com.virtuslab.ideprobe.jsonrpc.JsonRpcEndpoint
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class Probe(val connection: JsonRpcConnection)(implicit val ec: ExecutionContext) extends JsonRpcEndpoint {
  protected def handler: JsonRpc.Handler = ProbeHandlers.get()
}

object Probe {
  def start(socket: Socket)(implicit ec: ExecutionContext): Future[Unit] = {
    val connection = JsonRpcConnection.from(socket)
    val probe = new Probe(connection)

    Future(probe.listen).andThen(_ => Close(probe, socket))
  }
}
