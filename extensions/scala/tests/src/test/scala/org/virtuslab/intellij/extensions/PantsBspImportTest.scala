package org.virtuslab.intellij.extensions

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.junit.Assert
import org.junit.Test
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.Shell.CommandResult

class PantsBspImportTest extends ScalaPluginTestSuite {

  @Test def importTest(): Unit = {
    fixtureFromConfig().run { intelliJ =>
      val projectPath = createBspProjectWithFastpass(intelliJ.workspace, "java_app::", "scala_tests::")

      intelliJ.probe.openProject(projectPath)

      val project = intelliJ.probe.projectModel()

      Assert.assertEquals("Project name is incorrect", "java_app__scala_tests", project.name)
      val expectedModules = Set(
        "java_app:main-bin",
        "scala_tests:scala_tests",
        "java_app__scala_tests-root"
      )
      Assert.assertEquals("Module set is incorrect", expectedModules, project.modules.map(_.name).toSet)
    }
  }


  private def createBspProjectWithFastpass(workspace: Path, targets: String*): Path = {
    val args = Seq(
      "create",
      "--no-bloop-exit",
      "--intellij",
      "--intellijLauncher", "echo"
    ) ++ targets
    Paths.get(runFastpass(workspace, args).out)
  }

  // fastpass is as tool that can convert pants project into bsp project
  private def runFastpass(workspace: Path, args: Seq[String]): CommandResult = {
    val fastpassVersion = "1.1.1"
    val command = Seq(
      coursierPath.toString,
      "launch",
      s"org.scalameta:fastpass_2.12:$fastpassVersion",
      "-r", "sonatype:snapshots",
      "--main", "scala.meta.fastpass.Fastpass",
      "--"
    ) ++ args
    Shell.run(workspace, command: _*)
  }

  lazy val coursierPath: Path = {
    val destination = Paths.get(System.getProperty("java.io.tmpdir"), "ideprobe-coursier")
    if (!Files.exists(destination)) {
      val url = new URL("https://git.io/coursier-cli")
      Files.copy(url.openConnection.getInputStream, destination)
      destination.toFile.setExecutable(true)
    }
    destination
  }

}
