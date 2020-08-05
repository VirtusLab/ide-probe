package org.virtuslab.intellij.extensions

import org.junit.Assert
import org.junit.Test
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.Shell

import scala.util.Try

final class ScalaImportTest extends ScalaPluginTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    fixtureFromConfig(config).run { intellij =>
      val projectRef = intellij.probe.openProject(intellij.workspace.resolve("root"))
      val project = intellij.probe.projectModel(projectRef)
      val modules = project.modules.map(_.name).toSet
      val sdk = intellij.probe.projectSdk()

      Assert.assertTrue("SDK is empty", sdk.nonEmpty)
      Assert.assertEquals(Set("hello-world-build", "hello-world", "foo", "bar"), modules)
      Assert.assertEquals(project.name, "hello-world")
    }
  }

  @Test
  def importBspProject(): Unit = {
    fixtureFromConfig(config).run { intellij =>
      // Root directory is necessary, since bsp plugin creates project
      // inside the **parent** of the directory specified
      val root = intellij.workspace.resolve("root")

      root
        .resolve("project/plugins.sbt")
        .write("""addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.0-RC1")""")

      root
        .resolve(".bsp/bloop.json")
        .write(s"""|{
              |  "name": "Bloop",
              |  "version": "1.4.0-RC1",
              |  "bspVersion": "2.0.0-M4+10-61e61e87",
              |  "languages": ["scala", "java"],
              |  "argv": [
              |    "${intellij.workspace.resolve("bin/coursier")}",
              |    "launch",
              |    "ch.epfl.scala:bloop-launcher-core_2.12:1.4.0-RC1",
              |    "--",
              |    "1.4.0-RC1"
              |  ]
              |}
              |""".stripMargin)

      val result = Shell.run(in = root, "sbt", "bloopInstall")
      Assert.assertEquals(result.exitCode, 0)
      root.resolve("build.sbt").delete()
      root.resolve("project").delete()
      Try {
        root.resolve(".idea").delete()
      } // todo find out why it is here

      val projectRef = intellij.probe.openProject(root)
      val project = intellij.probe.projectModel(projectRef)

      val workspaceName = root.getFileName.toString
      val modules = project.modules.map(_.name).toSet
      Assert.assertEquals(Set("foo", "bar", workspaceName), modules)
      Assert.assertEquals(workspaceName, project.name)
    }
  }
}
