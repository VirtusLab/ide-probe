package org.virtuslab

import java.net.Socket
import org.virtuslab.ideprobe.Close
import org.virtuslab.ideprobe.jsonrpc.JsonRpc
import org.virtuslab.ideprobe.jsonrpc.JsonRpcConnection
import org.virtuslab.ideprobe.jsonrpc.JsonRpcEndpoint
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class Probe(val connection: JsonRpcConnection)(implicit val ec: ExecutionContext) extends JsonRpcEndpoint {
  protected def handler: JsonRpc.Handler = ProbeHandlers.get()
}

object Probe {
  def start(socket: Socket)(implicit ec: ExecutionContext): Future[Unit] = {
    val connection = JsonRpcConnection.from(socket)
    val probe = new Probe(connection)

    Future(probe.listen).andThen { case _ => Close(probe, socket) }
  }
}
