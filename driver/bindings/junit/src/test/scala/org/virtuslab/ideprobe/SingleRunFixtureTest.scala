package org.virtuslab.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.junit.Assert._
import org.junit.Assume
import org.junit.Test
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

final class SingleRunFixtureTest extends IntegrationTestSuite with WorkspaceFixture with Assertions {
  private val fixture = new SingleRunIntelliJ(IntelliJFixture())

  @Test // TODO use ProcessHandle when on java 9
  def shutdownsLauncherAfterTest(): Unit = {
    Assume.assumeFalse(OS.Current == OS.Windows)

    var pid: java.lang.Long = null

    fixture { intellij =>
      pid = intellij.probe.pid()
    }

    val result = Await.result(Shell.async("kill", "-0", s"$pid"), FiniteDuration(10, TimeUnit.SECONDS))
    if (result.exitCode == 0) fail("IDE process was not terminated")
  }

  @Test
  def removesDirectoriesAfterRunTest(): Unit = {
    var workspace: Path = null
    var ideHome: Path = null
    fixture { probe =>
      workspace = probe.workspace
      ideHome = probe.intelliJPaths.bin.getParent
    }

    assertFalse("Workspace was not removed", Files.exists(workspace))
    assertFalse("IDE home was not removed", Files.exists(ideHome))
  }
}
