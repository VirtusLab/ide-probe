package org.virtuslab.ideprobe.jsonrpc

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

import org.virtuslab.ideprobe.Close
import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Message

private[jsonrpc] final class Channel(socket: Socket) extends AutoCloseable {
  private val input = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))

  def send(message: Message): Unit = {
    JsonRpc.append(message, output)
    output.flush()
  }

  val received: Iterator[Message] = JsonRpc.stream(input)

  override def close(): Unit = {
    Close(input, output, socket)
  }
}
