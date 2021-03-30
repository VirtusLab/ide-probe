package org.virtuslab.ideprobe.junit4

import org.junit.AfterClass
import org.junit.BeforeClass
import org.virtuslab.ideprobe.RunningIntelliJPerSuiteBase
import org.virtuslab.ideprobe.WorkspacePerSuiteBase

trait RunningIntelliJPerSuite extends RunningIntelliJPerSuiteBase {
  @BeforeClass override final def setup(): Unit = super.setup()
  @AfterClass override final def teardown(): Unit = super.teardown()
}

trait WorkspacePerSuite extends WorkspacePerSuiteBase {
  @BeforeClass override final def setup(): Unit = super.setup()
  @AfterClass override final def teardown(): Unit = super.teardown()
}
