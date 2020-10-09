package org.virtuslab.ideprobe

import java.net.URL
import java.nio.charset.Charset
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import org.apache.commons.io.IOUtils
import org.junit.Assert._
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.Plugin
import org.virtuslab.ideprobe.jsonrpc.RemoteException
import org.virtuslab.ideprobe.protocol.BuildScope
import org.virtuslab.ideprobe.protocol.FileRef
import org.virtuslab.ideprobe.protocol.InstalledPlugin
import org.virtuslab.ideprobe.protocol.JUnitRunConfiguration
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.TestStatus
import org.virtuslab.ideprobe.protocol.TestStatus.Passed
import org.virtuslab.ideprobe.protocol.VcsRoot
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

@RunWith(classOf[JUnit4])
final class ProbeDriverTest extends IdeProbeFixture with Assertions with RobotPluginExtension {
  private val scalaPlugin = Plugin("org.intellij.scala", "2020.3.369", Some("nightly"))
  private val probeTestPlugin = ProbeTestPlugin.bundled

  private val fixture = IntelliJFixture(
    version = IntelliJVersion.Latest,
    plugins = List(scalaPlugin, probeTestPlugin)
  ).enableExtensions

  @Test
  def listsPlugins(): Unit = fixture.run { intelliJ =>
    val plugins = intelliJ.probe.plugins

    assertContains(plugins)(InstalledPlugin(scalaPlugin.id, scalaPlugin.version))
    assertExists(plugins)(plugin => plugin.id == ProbeTestPlugin.id)
  }

  @Test
  def collectErrors(): Unit = fixture.run { intelliJ =>
    intelliJ.probe.invokeActionAsync("org.virtuslab.ideprobe.test.ThrowingAction")
    val errors = intelliJ.probe.errors
    assertExists(errors)(error =>
      error.content.contains("ThrowingAction") && error.pluginId.contains(ProbeTestPlugin.id)
    )
  }

  @Test
  def projectOpen(): Unit =
    fixture
      .copy(workspaceProvider = WorkspaceTemplate.FromResource("OpenProjectTest"))
      .run { intelliJ =>
        val expectedProjectName = "empty-project"
        val projectPath = intelliJ.workspace.resolve(expectedProjectName)
        val actualProjectRef = intelliJ.probe.withRobot.openProject(projectPath)
        assertEquals(ProjectRef(expectedProjectName), actualProjectRef)
      }

  @Test
  def backgroundTask(): Unit = fixture.run { intelliJ =>
    // time it takes to verify that IDE is actually idle
    val errorMargin = 15.seconds

    assertDuration(min = 15.seconds, max = 5.minutes) {
      intelliJ.probe.invokeAction("org.virtuslab.ideprobe.test.BackgroundTaskAction15s")
    }
    assertDuration(max = errorMargin) {
      intelliJ.probe.awaitIdle()
    }
  }

  @Test
  def freezeInspector(): Unit = fixture.run { intelliJ =>
    intelliJ.probe.invokeAction("org.virtuslab.ideprobe.test.FreezingAction")
    val freezes = intelliJ.probe.freezes
    assertExists(freezes) { freeze =>
      freeze.duration.exists(_ >= 10.seconds) &&
      freeze.edtStackTrace.exists(frame => frame.contains("Thread.sleep")) &&
      freeze.edtStackTrace.exists(frame => frame.contains("FreezingAction.actionPerformed"))
    }
  }

  private val buildTestFixture = fixture.copy(workspaceProvider = WorkspaceTemplate.FromResource("BuildTest"))

  @Test
  def vcsDetection(): Unit = {
    fixture
      .copy(workspaceProvider = WorkspaceTemplate.FromResource("OpenProjectTest"))
      .withWorkspace { workspace =>
        val projectDir = workspace.path.resolve("empty-project")
        Shell.run(in = projectDir, "git", "init")
        workspace.runIntellij { intelliJ =>
          intelliJ.probe.withRobot.openProject(projectDir)
          val vcsRoots = intelliJ.probe.vcsRoots()
          assertEquals(Seq(VcsRoot("Git", projectDir)), vcsRoots)
        }
      }
  }

  @Test
  def expandsMacro(): Unit =
    fixture.copy(workspaceProvider = WorkspaceTemplate.FromResource("gradle-project")).run { intelliJ =>
      val projectRef = intelliJ.probe.withRobot.openProject(intelliJ.workspace)
      val fileExtension =
        intelliJ.probe
          .expandMacro("$FileExt$", FileRef(intelliJ.workspace.resolve("build.gradle"), projectRef))
      assertEquals("gradle", fileExtension)
    }

  @Test
  def listsAllSourceRoots(): Unit = {
    fixture.copy(workspaceProvider = WorkspaceTemplate.FromResource("gradle-project")).run { intelliJ =>
      val projectDir = intelliJ.workspace.resolve("build.gradle")
      val src = intelliJ.workspace.resolve("src")

      intelliJ.probe.withRobot.openProject(projectDir)

      val model = intelliJ.probe.projectModel()
      val mainModule = model.modules.find(_.name == "foo.main").get
      val testModule = model.modules.find(_.name == "foo.test").get

      assertEquals(Set(src.resolve("main/java")), mainModule.contentRoots.paths.sources)
      assertEquals(Set(src.resolve("main/resources")), mainModule.contentRoots.paths.resources)
      assertEquals(Set(src.resolve("test/java")), testModule.contentRoots.paths.testSources)
      assertEquals(Set(src.resolve("test/resources")), testModule.contentRoots.paths.testResources)
    }
  }

  @Test
  def listsModuleDependencies(): Unit = {
    fixture.copy(workspaceProvider = WorkspaceTemplate.FromResource("gradle-project")).run { intelliJ =>
      val projectDir = intelliJ.workspace.resolve("build.gradle")
      val p = intelliJ.probe.withRobot.openProject(projectDir)

      val testModule = intelliJ.probe.projectModel().modules.find(_.name == "foo.test").get
      assertEquals(Set(ModuleRef("foo.main", p)), testModule.dependencies)
    }
  }

  @Ignore
  @Test
  def buildProjectTest(): Unit = {
    buildTestFixture
      .run { intelliJ =>
        val projectDir = intelliJ.workspace.resolve("simple-sbt-project")

        intelliJ.probe.withRobot.openProject(projectDir)

        val successfulResult = intelliJ.probe.build()
        assertEquals(successfulResult.errors, Nil)

        projectDir.resolve("src/main/scala/Main.scala").write("Not valid scala")
        intelliJ.probe.syncFiles()

        val failedResult = intelliJ.probe.build()
        assertExists(failedResult.errors) { error =>
          error.file.exists(_.endsWith("src/main/scala/Main.scala")) &&
          error.content.contains("expected class or object definition")
        }
      }
  }

  @Ignore
  @Test
  def buildFilesTest(): Unit = {
    buildTestFixture.run { intelliJ =>
      val projectDir = intelliJ.workspace.resolve("simple-sbt-project")
      val project = intelliJ.probe.withRobot.openProject(projectDir)

      val compilingFile = projectDir.resolve("src/main/scala/Main.scala")
      val nonCompilingFile = projectDir.resolve("src/main/scala/Incorrect.scala")
      nonCompilingFile.write("incorrect")
      intelliJ.probe.syncFiles()

      val failedResult = intelliJ.probe.build(BuildScope.files(project, nonCompilingFile))
      val successfulResult = intelliJ.probe.build(BuildScope.files(project, compilingFile))

      assertExists(failedResult.errors)(error => error.file.exists(_.endsWith("Incorrect.scala")))
      assertEquals(Nil, successfulResult.errors)
    }
  }

  @Ignore
  @Test
  def runTests(): Unit = {
    buildTestFixture
      .run { intelliJ =>
        val projectDir = intelliJ.workspace.resolve("simple-sbt-project")

        intelliJ.probe.withRobot.openProject(projectDir)

        intelliJ.probe.build()
        val configuration = JUnitRunConfiguration.module(ModuleRef("simple-sbt-project"))
        val result = intelliJ.probe.run(configuration)

        assertFalse(result.isSuccess)

        assertEquals("Unexpected number of suites", 1, result.suites.size)

        val suite = result.suites.head
        assertEquals("ExampleTest", suite.name)

        val Seq(testA, testB, testC, testD) = suite.tests

        assertEquals("testA", testA.name)
        assertEquals(Passed, testA.status)

        assertEquals("testB", testB.name)
        testB.status match {
          case TestStatus.Failed(errorMessage) => assertTrue(errorMessage.contains("java.lang.AssertionError"))
          case _                               => fail()
        }

        assertEquals("testC", testC.name)
        assertEquals(testC.status, TestStatus.Ignored)

        assertEquals("testD", testD.name)
        testD.status match {
          case TestStatus.Failed(errorMessage) => assertTrue(errorMessage.contains("java.lang.OutOfMemoryError"))
          case _                               => fail()
        }
      }
  }

  @Test
  def startsUsingCustomCommand(): Unit = {
    IntelliJFixture.fromConfig(Config.fromClasspath("CustomCommand/ideprobe.conf")).run { intelliJ =>
      val output = intelliJ.workspace.resolve("output")
      assertTrue(s"Not a file $output", output.isFile)
    }
  }

  @Ignore
  @Test
  def failsOnUnexpectedWindow(): Unit = fixture.run { intellij =>
    val action = "org.virtuslab.ideprobe.test.OpenWindowAction"
    Try(intellij.probe.invokeAction(action)) match {
      case Failure(e: RemoteException) if e.getMessage.contains("Unexpectedly opened window") => ()

      case Success(_) => fail("Opened window did not trigger an error")
      case Failure(e) => throw e
    }
  }

  @Test
  def robotTest(): Unit = fixture.run { intelliJ =>
    val version = fixture.version.inferredMajor
    val welcomeFrame = intelliJ.probe.withRobot.find(query.className("FlatWelcomeFrame"))
    val windowText = welcomeFrame.fullText
    assertTrue(s"Window content: '$windowText' did not contain '$version'", windowText.contains(version))

    val newProjectDialog = retry(3) {
      Try {
        welcomeFrame.actionLink("New Project").click()
      }.getOrElse {
        // fallback for 2020.3
        welcomeFrame.find(query.button("selectedicon" -> "createNewProjectTabSelected.svg")).click()
      }
      intelliJ.probe.withRobot.find(query.dialog("New Project"))
    }
    val dialogContent = newProjectDialog.fullText

    val projectSdk = "Project SDK"
    assertTrue(
      s"New Project dialog content: '$dialogContent' did not contain '$projectSdk'",
      dialogContent.contains(projectSdk)
    )
  }

  // temporary for debugging
  @tailrec
  private def retry[A](times: Int)(action: => A): A = {
    try {
      action
    } catch {
      case e: WaitForConditionTimeoutException =>
        if (times > 0) {
          println("Failed to find element, retrying...")
          e.printStackTrace()
          try println(IOUtils.toString(new URL("http://localhost:9534/"), Charset.defaultCharset()))
          catch {
            case e: Exception => e.printStackTrace()
          }
          retry(times - 1)(action)
        } else {
          throw e
        }
    }
  }
}
