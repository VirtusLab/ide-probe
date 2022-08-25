package org.virtuslab.ideprobe.dependencies

import scala.io.Source.fromResource

import org.junit.Assert._
import org.junit.Test

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.CheckConfig
import org.virtuslab.ideprobe.config.CheckConfig.ErrorConfig
import org.virtuslab.ideprobe.protocol.IdeMessage
import org.virtuslab.ideprobe.protocol.IdeMessage.Level
import org.virtuslab.ideprobe.reporting.ErrorValidator

class ErrorValidatorTest {
  private val configRoot = "probe"
  private val defaultConfigWithErrorsEnabled: Config = Config.fromClasspath("error_validator.conf")
  private val defaultCheckConfigWithErrorsEnabled: CheckConfig =
    IntelliJFixture.readIdeProbeConfig(defaultConfigWithErrorsEnabled, configRoot).driver.check

  private val messageBusConnectionImplNPE = fromResource("ideError_1.txt").mkString
  private val uastMetaLanguageNPE = fromResource("ideError_2.txt").mkString

  private val errorIdeMessages: List[IdeMessage] = List(messageBusConnectionImplNPE, uastMetaLanguageNPE)
    .map(content => IdeMessage(level = Level.Error, content, None))

  private val messageBusSpecificString = "at com.intellij.util.messages.impl.MessageBusConnectionImpl"
  private val uastMetaSpecificString = "at com.intellij.uast.UastMetaLanguage"

  @Test
  def shouldIncludeAllErrorsByDefault(): Unit = {
    val exceptions: List[Option[Exception]] = collectExceptions(defaultCheckConfigWithErrorsEnabled)
    val exceptionsIncluded = exceptions.filter(_.nonEmpty)
    val exceptionsIgnored = exceptions.filter(_.isEmpty)
    assertEquals(2, exceptionsIncluded.size)
    assertEquals(0, exceptionsIgnored.size)
  }

  @Test
  def shouldIgnoreAllErrorsIfNotEnabled(): Unit = {
    val checkConfigErrorsDisabled = defaultCheckConfigWithErrorsEnabled.copy(errors =
      ErrorConfig(
        enabled = false,
        includeMessages = defaultCheckConfigWithErrorsEnabled.errors.includeMessages,
        excludeMessages = defaultCheckConfigWithErrorsEnabled.errors.excludeMessages
      )
    )
    val exceptions: List[Option[Exception]] = collectExceptions(checkConfigErrorsDisabled)
    val exceptionsIncluded = exceptions.filter(_.nonEmpty)
    val exceptionsIgnored = exceptions.filter(_.isEmpty)
    assertEquals(0, exceptionsIncluded.size)
    assertEquals(2, exceptionsIgnored.size)
  }

  @Test
  def shouldIncludeOnlySpecificErrorsIfIncludeMessagesConfigured(): Unit = {
    val checkConfigForMessageBusErrorsOnly = defaultCheckConfigWithErrorsEnabled.copy(errors =
      ErrorConfig(
        enabled = defaultCheckConfigWithErrorsEnabled.errors.enabled,
        includeMessages = Seq(s".*$messageBusSpecificString.*"),
        excludeMessages = defaultCheckConfigWithErrorsEnabled.errors.excludeMessages
      )
    )
    val exceptions: List[Option[Exception]] = collectExceptions(checkConfigForMessageBusErrorsOnly)
    val exceptionsIncluded = exceptions.filter(_.nonEmpty)
    val exceptionsIgnored = exceptions.filter(_.isEmpty)
    assertEquals(1, exceptionsIncluded.size)
    assertEquals(1, exceptionsIgnored.size)
    assertTrue(exceptionsIncluded.head.get.getMessage.contains(messageBusSpecificString))
  }

  @Test
  def shouldExcludeSpecificErrorsByRegexUsage(): Unit = {
    val checkConfigToIgnoreMessageBusErrors = defaultCheckConfigWithErrorsEnabled.copy(errors =
      ErrorConfig(
        enabled = defaultCheckConfigWithErrorsEnabled.errors.enabled,
        includeMessages = defaultCheckConfigWithErrorsEnabled.errors.includeMessages,
        excludeMessages = Seq(s".*$messageBusSpecificString.*")
      )
    )
    val exceptions: List[Option[Exception]] = collectExceptions(checkConfigToIgnoreMessageBusErrors)
    val exceptionsIncluded = exceptions.filter(_.nonEmpty)
    val exceptionsIgnored = exceptions.filter(_.isEmpty)
    assertEquals(1, exceptionsIncluded.size)
    assertEquals(1, exceptionsIgnored.size)
    assertFalse(exceptionsIncluded.head.get.getMessage.contains(messageBusSpecificString))
    assertTrue(exceptionsIncluded.head.get.getMessage.contains(uastMetaSpecificString))
  }

  @Test
  def shouldExcludeErrorsByPartsOfStackTrace(): Unit = {
    val partOfMessageBusStackTrace =
      """
        |at com.intellij.util.messages.impl.MessageBusConnectionImpl.deliverImmediately(MessageBusConnectionImpl.java:61)
        |    	at com.intellij.psi.impl.file.impl.FileManagerImpl.dispatchPendingEvents(FileManagerImpl.java:311)
        |    	at com.intellij.psi.impl.file.impl.FileManagerImpl.getCachedPsiFile(FileManagerImpl.java:378)
        |    	at com.intellij.psi.impl.PsiDocumentManagerBase.getCachedPsiFile(PsiDocumentManagerBase.java:143)
        |""".stripMargin
    val checkConfigToIgnoreMessageBusErrors = defaultCheckConfigWithErrorsEnabled.copy(errors =
      ErrorConfig(
        enabled = defaultCheckConfigWithErrorsEnabled.errors.enabled,
        includeMessages = defaultCheckConfigWithErrorsEnabled.errors.includeMessages,
        excludeMessages = Seq(partOfMessageBusStackTrace)
      )
    )
    val exceptions: List[Option[Exception]] = collectExceptions(checkConfigToIgnoreMessageBusErrors)
    val exceptionsIncluded = exceptions.filter(_.nonEmpty)
    val exceptionsIgnored = exceptions.filter(_.isEmpty)
    assertEquals(1, exceptionsIncluded.size)
    assertEquals(1, exceptionsIgnored.size)
    assertFalse(exceptionsIncluded.head.get.getMessage.contains(messageBusSpecificString))
    assertTrue(exceptionsIncluded.head.get.getMessage.contains(uastMetaSpecificString))
  }

  private def collectExceptions(checkConfig: CheckConfig): List[Option[Exception]] =
    errorIdeMessages.map(msg => ErrorValidator(checkConfig, Seq(msg)))

}
