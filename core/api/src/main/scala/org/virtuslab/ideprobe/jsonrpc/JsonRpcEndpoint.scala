package org.virtuslab.ideprobe.jsonrpc

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.virtuslab.ideprobe.Close
import org.virtuslab.ideprobe.jsonrpc.JsonRpc._
import org.virtuslab.ideprobe.jsonrpc.logging.ProbeCommunicationLogger

trait JsonRpcEndpoint extends AutoCloseable {
  protected def logger: ProbeCommunicationLogger = ProbeCommunicationLogger.empty
  protected def connection: JsonRpcConnection

  implicit protected def ec: ExecutionContext

  protected def handler: Handler

  protected def sendRequest[A, B](method: Method[A, B], parameters: A): Future[B] = {
    Future(method.encode(parameters)).flatMap { json =>
      method match {
        case Method.Notification(name) =>
          logger.logRequest(name, json)
          connection.sendNotification(name, json)
          Future.unit.asInstanceOf[Future[B]]
        case Method.Request(name) =>
          logger.logRequest(name, json)
          connection
            .sendRequest(name, json)
            .flatMap {
              case JsonRpc.Failure(error) =>
                val exception = {
                  val cause = new Exception(error.message)
                  val stackTrace = JsonRpc.gson.fromJson(error.data, classOf[Array[StackTraceElement]])
                  cause.setStackTrace(stackTrace)
                  new RemoteException(cause)
                }

                logger.logResponse(error.message)
                Future.failed(exception)
              case response =>
                logger.logResponse(response.result)
                Future.successful(method.decode(response.result))
            }
      }
    }
  }

  lazy val listen: Unit =
    connection.onRequest { request =>
      handler(request.method, request.params)
        .map(connection.sendResponse(request, _))
        .recover { case cause => connection.sendError(request, cause) }
    }

  def close(): Unit = {
    Close(connection)
  }
}
