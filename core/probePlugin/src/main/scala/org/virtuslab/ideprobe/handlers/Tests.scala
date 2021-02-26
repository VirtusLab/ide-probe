package org.virtuslab.ideprobe
package handlers

import com.intellij.execution.testframework.sm.runner.{SMTRunnerEventsAdapter, SMTRunnerEventsListener, SMTestProxy}
import com.intellij.openapi.project.Project
import java.util.concurrent.CountDownLatch
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.{TestRun, TestStatus, TestSuite, TestsRunResult}

object Tests {
  def awaitTestResults(project: Project, launch: () => Unit): TestsRunResult = {
    val latch = new CountDownLatch(1)
    var testProxy: SMTestProxy.SMRootTestProxy = null
    project.getMessageBus
      .connect()
      .subscribe(
        SMTRunnerEventsListener.TEST_STATUS,
        new SMTRunnerEventsAdapter() {
          override def onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy): Unit = {
            testProxy = testsRoot
            latch.countDown()
          }
        }
      )

    launch()
    latch.await()

    def createSuite(suiteProxy: SMTestProxy) = {
      val tests = suiteProxy.getChildren.asScala.map { testProxy =>
        val status =
          if (testProxy.isPassed) TestStatus.Passed
          else if (testProxy.isIgnored) TestStatus.Ignored
          else TestStatus.Failed(testProxy.getErrorMessage + testProxy.getStacktrace)
        TestRun(testProxy.getPresentableName, testProxy.getDuration, status)
      }.toList
      TestSuite(suiteProxy.getPresentableName, tests)
    }

    val suites = if (testProxy.getChildren.asScala.exists(_.isLeaf)) {
      Seq(createSuite(testProxy))
    } else {
      testProxy.getChildren.asScala.map(createSuite).toList
    }

    TestsRunResult(suites)
  }

}
