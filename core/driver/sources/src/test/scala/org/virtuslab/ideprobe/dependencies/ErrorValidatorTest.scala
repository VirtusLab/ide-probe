package org.virtuslab.ideprobe.dependencies

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

  private val messageBusConnectionImplNPE =
    """
      |java.lang.NullPointerException
      |    	at com.intellij.util.messages.impl.MessageBusConnectionImpl.deliverImmediately(MessageBusConnectionImpl.java:61)
      |    	at com.intellij.psi.impl.file.impl.FileManagerImpl.dispatchPendingEvents(FileManagerImpl.java:311)
      |    	at com.intellij.psi.impl.file.impl.FileManagerImpl.getCachedPsiFile(FileManagerImpl.java:378)
      |    	at com.intellij.psi.impl.PsiDocumentManagerBase.getCachedPsiFile(PsiDocumentManagerBase.java:143)
      |    	at com.intellij.psi.impl.PsiDocumentManagerImpl$1.lambda$fileContentLoaded$0(PsiDocumentManagerImpl.java:55)
      |    	at com.intellij.openapi.application.impl.ApplicationImpl.runReadAction(ApplicationImpl.java:865)
      |    	at com.intellij.openapi.application.ReadAction.compute(ReadAction.java:61)
      |    	at com.intellij.psi.impl.PsiDocumentManagerImpl$1.fileContentLoaded(PsiDocumentManagerImpl.java:55)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.invokeMethod(MessageBusImpl.java:645)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.invokeListener(MessageBusImpl.java:620)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.deliverMessage(MessageBusImpl.java:417)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.pumpWaitingBuses(MessageBusImpl.java:390)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.pumpMessages(MessageBusImpl.java:372)
      |    	at com.intellij.util.messages.impl.MessageBusImpl.access$200(MessageBusImpl.java:33)
      |    	at com.intellij.util.messages.impl.MessageBusImpl$MessagePublisher.invoke(MessageBusImpl.java:179)
      |    	at com.sun.proxy.$Proxy137.suspendableProgressAppeared(Unknown Source)
      |    	at com.intellij.openapi.progress.impl.ProgressSuspender.<init>(ProgressSuspender.java:61)
      |    	at com.intellij.openapi.progress.impl.ProgressSuspender.markSuspendable(ProgressSuspender.java:77)
      |    	at com.intellij.openapi.project.DumbServiceImpl.runBackgroundProcess(DumbServiceImpl.java:604)
      |    	at com.intellij.openapi.project.DumbServiceImpl$5.run(DumbServiceImpl.java:587)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.startTask(CoreProgressManager.java:436)
      |    	at com.intellij.openapi.progress.impl.ProgressManagerImpl.startTask(ProgressManagerImpl.java:120)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.lambda$runProcessWithProgressAsync$5(CoreProgressManager.java:496)
      |    	at com.intellij.openapi.progress.impl.ProgressRunner.lambda$submit$3(ProgressRunner.java:244)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.lambda$runProcess$2(CoreProgressManager.java:188)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.lambda$executeProcessUnderProgress$12(CoreProgressManager.java:624)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.registerIndicatorAndRun(CoreProgressManager.java:698)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.computeUnderProgress(CoreProgressManager.java:646)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.executeProcessUnderProgress(CoreProgressManager.java:623)
      |    	at com.intellij.openapi.progress.impl.ProgressManagerImpl.executeProcessUnderProgress(ProgressManagerImpl.java:66)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.runProcess(CoreProgressManager.java:175)
      |    	at com.intellij.openapi.progress.impl.ProgressRunner.lambda$submit$4(ProgressRunner.java:244)
      |    	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1700)
      |    	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
      |    	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
      |    	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1$1.run(Executors.java:668)
      |    	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1$1.run(Executors.java:665)
      |    	at java.base/java.security.AccessController.doPrivileged(Native Method)
      |    	at java.base/java.util.concurrent.Executors$PrivilegedThreadFactory$1.run(Executors.java:665)
      |    	at java.base/java.lang.Thread.run(Thread.java:829)
      |""".stripMargin

  private val uastMetaLanguageNPE =
    """
      |java.lang.NullPointerException
      |    	at com.intellij.uast.UastMetaLanguage.matchesLanguage(UastMetaLanguage.java:38)
      |    	at com.intellij.codeInsight.daemon.impl.JavaColorProvider.getColorFrom(JavaColorProvider.java:39)
      |    	at com.intellij.ui.ColorLineMarkerProvider.lambda$getLineMarkerInfo$0(ColorLineMarkerProvider.java:39)
      |    	at com.intellij.openapi.extensions.impl.ExtensionProcessingHelper.computeSafeIfAny(ExtensionProcessingHelper.java:55)
      |    	at com.intellij.openapi.extensions.ExtensionPointName.computeSafeIfAny(ExtensionPointName.java:57)
      |    	at com.intellij.ui.ColorLineMarkerProvider.getLineMarkerInfo(ColorLineMarkerProvider.java:38)
      |    	at com.intellij.codeInsight.daemon.impl.LineMarkersPass.queryProviders(LineMarkersPass.java:158)
      |    	at com.intellij.codeInsight.daemon.impl.LineMarkersPass.lambda$doCollectInformation$3(LineMarkersPass.java:83)
      |    	at com.intellij.codeInsight.daemon.impl.Divider.divideInsideAndOutsideInOneRoot(Divider.java:81)
      |    	at com.intellij.codeInsight.daemon.impl.LineMarkersPass.doCollectInformation(LineMarkersPass.java:78)
      |    	at com.intellij.codeHighlighting.TextEditorHighlightingPass.collectInformation(TextEditorHighlightingPass.java:56)
      |    	at com.intellij.codeInsight.daemon.impl.PassExecutorService$ScheduledPass.lambda$doRun$1(PassExecutorService.java:400)
      |    	at com.intellij.openapi.application.impl.ApplicationImpl.tryRunReadAction(ApplicationImpl.java:1137)
      |    	at com.intellij.codeInsight.daemon.impl.PassExecutorService$ScheduledPass.lambda$doRun$2(PassExecutorService.java:393)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.registerIndicatorAndRun(CoreProgressManager.java:658)
      |    	at com.intellij.openapi.progress.impl.CoreProgressManager.executeProcessUnderProgress(CoreProgressManager.java:610)
      |    	at com.intellij.openapi.progress.impl.ProgressManagerImpl.executeProcessUnderProgress(ProgressManagerImpl.java:65)
      |    	at com.intellij.codeInsight.daemon.impl.PassExecutorService$ScheduledPass.doRun(PassExecutorService.java:392)
      |    	at com.intellij.codeInsight.daemon.impl.PassExecutorService$ScheduledPass.lambda$run$0(PassExecutorService.java:368)
      |    	at com.intellij.openapi.application.impl.ReadMostlyRWLock.executeByImpatientReader(ReadMostlyRWLock.java:172)
      |    	at com.intellij.openapi.application.impl.ApplicationImpl.executeByImpatientReader(ApplicationImpl.java:183)
      |    	at com.intellij.codeInsight.daemon.impl.PassExecutorService$ScheduledPass.run(PassExecutorService.java:366)
      |    	at com.intellij.concurrency.JobLauncherImpl$VoidForkJoinTask$1.exec(JobLauncherImpl.java:188)
      |    	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
      |    	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
      |    	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
      |    	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
      |    	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
      |""".stripMargin

  private val errorIdeMessages: List[IdeMessage] = List(messageBusConnectionImplNPE, uastMetaLanguageNPE)
    .map(content => IdeMessage(Level.Error, content, pluginId = None))

  private val messageBusSpecificString = "at com.intellij.util.messages.impl.MessageBusConnectionImpl"
  private val uastMetaSpecificString = "at com.intellij.uast.UastMetaLanguage"

  @Test
  def shouldIncludeAllErrorsByDefault(): Unit = {
    val exceptions: List[Option[Exception]] = collectExceptions(defaultCheckConfigWithErrorsEnabled)
    val (exceptionsIncluded, exceptionsIgnored) = exceptions.partition(_.nonEmpty)
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
    val (exceptionsIncluded, exceptionsIgnored) = exceptions.partition(_.nonEmpty)
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
    val (exceptionsIncluded, exceptionsIgnored) = exceptions.partition(_.nonEmpty)
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
    val (exceptionsIncluded, exceptionsIgnored) = exceptions.partition(_.nonEmpty)
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
    val (exceptionsIncluded, exceptionsIgnored) = exceptions.partition(_.nonEmpty)
    assertEquals(1, exceptionsIncluded.size)
    assertEquals(1, exceptionsIgnored.size)
    assertFalse(exceptionsIncluded.head.get.getMessage.contains(messageBusSpecificString))
    assertTrue(exceptionsIncluded.head.get.getMessage.contains(uastMetaSpecificString))
  }

  private def collectExceptions(checkConfig: CheckConfig): List[Option[Exception]] =
    errorIdeMessages.map(msg => ErrorValidator(checkConfig, Seq(msg)))

}
