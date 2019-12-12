package org.virtuslab

import org.virtuslab.handlers.IntelliJApi
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.util.Try

object CurrentRequest extends IntelliJApi {
  private implicit val ec: ExecutionContext = IdeProbeService.executionContext

  @volatile private var response: Promise[_] = _
  private val errors = mutable.Buffer[Throwable]()

  def fail(cause: Throwable): Unit = {
    if (response == null || !response.tryFailure(cause)) { // null only before first request received
      errors += cause
    }
  }

  def process[A](action: => A): A = {
    val promise = Promise[A]()
    response = promise // response must be set before starting execution as errors are handled asynchronously

    if (errors.nonEmpty) promise.failure(error)
    else ec.execute(() => promise.tryComplete(Try(action)))

    await(promise.future)
  }

  private def error: Exception = {
    val error = new Exception("An error occurred since last request")
    errors.foreach(error.addSuppressed)
    error
  }
}
