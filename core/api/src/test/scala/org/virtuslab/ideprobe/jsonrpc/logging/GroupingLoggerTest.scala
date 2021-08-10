package org.virtuslab.ideprobe.jsonrpc.logging

import org.junit.{Assert, Test}

import java.io.ByteArrayOutputStream

class GroupingLoggerTest {
  private val endpoint = "endpoint"
  private val response = "response"
  private val param = "param"
  private val differentEndpoint = "endpoint/different"
  private val differentResponse = "different response"

  @Test
  def firstDifferentRequestShouldCauseTheBufferToBePrinted(): Unit = withStdout { stdout =>
    val logger = createLogger()
    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    logger.logRequest(differentEndpoint, param)
    Assert.assertEquals(
      s"""Repeated 2 times in less than one second: {
        |  request[$endpoint]: $param
        |  response: $response
        |}
        |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def firstDifferentResponseShouldCauseTheBufferToBePrinted(): Unit = withStdout { stdout =>
    val logger = createLogger()

    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    logger.logRequest(endpoint, param)
    logger.logResponse(differentResponse)

    Assert.assertEquals(
      s"""Repeated 2 times in less than one second: {
         |  request[$endpoint]: $param
         |  response: $response
         |}
         |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def responseWithoutPrecedingRequestShouldBePrintedImmediately(): Unit = withStdout { stdout =>
    val logger = createLogger()

    logger.logResponse(response)

    Assert.assertEquals(
      s"response: $response\n",
      stdout.toString
    )
  }

  @Test
  def twoDifferentRequestsWithoutResponseInBetweenShouldCauseTheFirstRequestToBePrinted(): Unit = withStdout { stdout =>
    val logger = createLogger()

    logger.logRequest(endpoint, param)
    logger.logRequest(differentEndpoint, param)

    Assert.assertEquals(
      s"request[$endpoint]: $param\n",
      stdout.toString
    )
  }

  @Test
  def twoRepeatedRequestsWithoutResponseInBetweenShouldCauseTheFirstRequestToBePrinted(): Unit = withStdout { stdout =>
    val logger = createLogger()
    logger.logRequest(endpoint, param)
    logger.logRequest(endpoint, param)

    Assert.assertEquals(
      s"request[$endpoint]: $param\n",
      stdout.toString
    )
  }

  @Test
  def messagesShouldBePrintedAfterSpecifiedAmountOfTimePasses(): Unit = withStdout { stdout =>
    val logger = createLogger()
    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    logger.logRequest(endpoint, param)
    logger.logResponse(response)
    Thread.sleep(1200L)

    Assert.assertEquals(
      s"""Repeated 2 times over one second: {
         |  request[$endpoint]: $param
         |  response: $response
         |}
         |""".stripMargin,
      stdout.toString
    )
  }

  @Test
  def skipLoggingIgnoredMessagesAlongWithResponse(): Unit = withStdout { stdout =>
    val logger = createLogger(ignore = Seq("request\\[config/set\\]"))

    logger.logRequest("config/set", param)
    Thread.sleep(500L)
    logger.logResponse(response)
    Thread.sleep(1200L)

    Assert.assertEquals(
      "",
      stdout.toString
    )
  }

  @Test
  def skipLoggingIgnoredMessagesAlongWithResponseWhenRequestWasFlushed(): Unit = withStdout { stdout =>
    val logger = createLogger(ignore = Seq("request\\[config/set\\]"))

    logger.logRequest("config/set", param)
    Thread.sleep(1200L)
    logger.logResponse("configured")
    Thread.sleep(1200L)

    Assert.assertEquals(
      "",
      stdout.toString
    )
  }

  private def withStdout(test: ByteArrayOutputStream => Unit): Unit = {
    val output = new ByteArrayOutputStream
    Console.withOut(output)(test(output))
  }

  private def createLogger(ignore: Seq[String] = Nil) = {
    new GroupingLogger(LoggingConfig(blockList = ignore))
  }

}
