package org.virtuslab.ideprobe.dependencies

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

import org.junit.Assert._
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import org.virtuslab.ideprobe.Assertions
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.OS
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.SingleRunIntelliJ

@RunWith(classOf[JUnit4])
final class SingleRunFixtureTest extends IdeProbeFixture with WorkspaceFixture with Assertions {
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

  @Test
  def removesDirectoriesEvenAfterFailureToRunIntelliJ(): Unit = {
    val intellijLauncher = if (OS.Current == OS.Mac) "idea" else "idea.sh"
    val intelliJFixture = IntelliJFixture().withAfterIntelliJInstall((_, intellij) =>
      Files.delete(intellij.paths.root.resolve("bin").resolve(intellijLauncher)) // To prevent the IDE from launching.
    )

    val instancesDir = intelliJFixture.intelliJProvider.paths.instances
    val workspacesDir = intelliJFixture.intelliJProvider.paths.workspaces

    val instancesBefore = if (instancesDir.isDirectory) instancesDir.directChildren() else List.empty
    val workspacesBefore = if (workspacesDir.isDirectory) workspacesDir.directChildren() else List.empty

    val fixture = new SingleRunIntelliJ(intelliJFixture)

    try {
      fixture { _ =>
        // Empty, as we wouldn't be able to execute anything here.
      }
    } catch {
      case _: Exception => // Pass, we don't care what went wrong specifically.
    }

    val instancesNotDeleted = instancesDir.directChildren().diff(instancesBefore)
    val workspacesNotDeleted = workspacesDir.directChildren().diff(workspacesBefore)

    assertTrue(
      s"Expected instances directory cleanup, but ${instancesNotDeleted.mkString(",")} were not removed",
      instancesNotDeleted.isEmpty
    )

    assertTrue(
      s"Expected workspaces directory cleanup, but ${workspacesNotDeleted.mkString(",")} were not removed",
      workspacesNotDeleted.isEmpty
    )
  }

  @Test
  def copiesLogsIntoConfiguredDirectoryBeforeCleanup(): Unit = {
    val tmpDirString = "/tmp/ide-probe-test"
    val config = Config.fromString(s"""probe.paths.logExport = $tmpDirString""")
    val fixtureFromConfig = IntelliJFixture.fromConfig(config)
    val logExportFixture = new SingleRunIntelliJ(fixtureFromConfig)

    logExportFixture { _ =>
      // nothing special to be done here, invoked just for cleanup
    }

    val filesAndDirsFromLogExportDirectory = Paths.get(tmpDirString).recursiveChildren()

    assertTrue(filesAndDirsFromLogExportDirectory.exists { path =>
      path.name == "logs" && path.isDirectory && path.directChildren().nonEmpty
    })
    assertTrue(filesAndDirsFromLogExportDirectory.exists { path =>
      path.name == "idea.log" && path.content().nonEmpty
    })
  }

}
