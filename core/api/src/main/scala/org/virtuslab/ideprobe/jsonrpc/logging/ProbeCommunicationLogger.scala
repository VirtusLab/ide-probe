package org.virtuslab.ideprobe.jsonrpc.logging

trait ProbeCommunicationLogger {
  def logRequest(name: String, param: String): Unit
  def logResponse(result: String): Unit
  def close(): Unit = ()
}

object ProbeCommunicationLogger {
  val empty = new ProbeCommunicationLogger {
    override def logRequest(name: String, param: String): Unit = ()
    override def logResponse(result: String): Unit = ()
  }
}
