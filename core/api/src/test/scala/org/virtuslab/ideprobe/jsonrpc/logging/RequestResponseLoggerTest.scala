package org.virtuslab.ideprobe.jsonrpc.logging

import org.junit.{Assert, Test}

import java.io.ByteArrayOutputStream

class RequestResponseLoggerTest {
  private val request = "request"
  private val response = "response"
  private val differentRequest = "different request"
  private val differentResponse = "different response"

  @Test
  def firstDifferentRequestShouldCauseTheBufferToBePrinted(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger
    logger.logRequest(request)
    logger.logResponse(response)
    logger.logRequest(request)
    logger.logResponse(response)
    logger.logRequest(differentRequest)
    Assert.assertEquals(
      s"""Repeated 2 times in less than one second: {
        |  $request
        |  $response
        |}
        |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def firstDifferentResponseShouldCauseTheBufferToBePrinted(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger

    logger.logRequest(request)
    logger.logResponse(response)
    logger.logRequest(request)
    logger.logResponse(response)
    logger.logRequest(request)
    logger.logResponse(differentResponse)

    Assert.assertEquals(
      s"""Repeated 2 times in less than one second: {
         |  $request
         |  $response
         |}
         |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def responseWithoutPrecedingRequestShouldBePrintedImmediately(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger

    logger.logResponse(response)

    Assert.assertEquals(
      s"$response\n",
      stdout.toString
    )
  }

  @Test
  def twoDifferentRequestsWithoutResponseInBetweenShouldCauseTheFirstRequestToBePrinted(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger

    logger.logRequest(request)
    logger.logRequest(differentRequest)

    Assert.assertEquals(
      s"$request\n",
      stdout.toString
    )
  }

  @Test
  def twoRepeatedRequestsWithoutResponseInBetweenShouldCauseTheFirstRequestToBePrinted(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger
    logger.logRequest(request)
    logger.logRequest(request)

    Assert.assertEquals(
      s"$request\n",
      stdout.toString
    )
  }

  @Test
  def messagesShouldBePrintedAfterSpecifiedAmountOfTimePasses(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger
    logger.logRequest(request)
    logger.logResponse(response)
    logger.logRequest(request)
    logger.logResponse(response)
    Thread.sleep(1200L)

    Assert.assertEquals(
      s"""Repeated 2 times over one second: {
                   |  $request
                   |  $response
                   |}
                   |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def loggingNewRequestShouldResetTheTimer(): Unit = withStdout { stdout =>
    val logger = new RequestResponseLogger

    logger.logRequest(request)
    Thread.sleep(500L)
    logger.logResponse(response)
    Thread.sleep(500L)
    logger.logRequest(request)
    Thread.sleep(500L)
    logger.logResponse(response)
    Thread.sleep(500L)
    logger.logRequest(request)
    Thread.sleep(500L)
    logger.logResponse(response)
    Thread.sleep(1200L)

    Assert.assertEquals(
      s"""Repeated 3 times over 3 seconds: {
         |  $request
         |  $response
         |}
         |""".stripMargin,
      stdout.toString
    )
  }

  def withStdout(test: ByteArrayOutputStream => Unit): Unit = {
    val output = new ByteArrayOutputStream
    Console.withOut(output)(test(output))
  }
}
