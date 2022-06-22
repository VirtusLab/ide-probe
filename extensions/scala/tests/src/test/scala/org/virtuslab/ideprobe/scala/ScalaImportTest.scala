package org.virtuslab.ideprobe.scala

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}

import scala.util.Try

@RunWith(classOf[Parameterized])
final class ScalaImportTest(val scalaPlugin: Plugin.Versioned, val intellijVersion: IntelliJVersion)
  extends ScalaPluginTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    fixtureFromConfig(config)
      .withVersion(intellijVersion)
      .withPlugin(scalaPlugin)
      .run { intellij =>
        val projectRef = intellij.probe.withRobot.openProject(intellij.workspace.resolve("root"))
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
        .write("""addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.0")""")

      root
        .resolve(".bsp/bloop.json")
        .write(s"""|{
              |  "name": "Bloop",
              |  "version": "1.5.0",
              |  "bspVersion": "2.0.0-M4+10-61e61e87",
              |  "languages": ["scala", "java"],
              |  "argv": [
              |    "${intellij.workspace.resolve("bin/coursier")}",
              |    "launch",
              |    "ch.epfl.scala:bloop-launcher-core_2.13:1.5.0",
              |    "--",
              |    "1.5.0"
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

      val projectRef = intellij.probe.withRobot.openProject(root)
      val project = intellij.probe.projectModel(projectRef)

      val workspaceName = root.getFileName.toString
      val modules = project.modules.map(_.name).toSet
      Assert.assertEquals(Set("foo", "bar", workspaceName), modules)
      Assert.assertEquals(workspaceName, project.name)
    }
  }
}

object ScalaImportTest extends ProbeDriverTestParamsProvider