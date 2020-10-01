package org.virtuslab.ideprobe.scala

import java.net.URL
import java.nio.file.{Files, Path, Paths}
import org.virtuslab.ideprobe.dependencies.{Jdks, ResourceProvider}
import org.virtuslab.ideprobe.{IdeProbeFixture, Shell}
import org.virtuslab.ideprobe.junit4.IdeProbeTestSuite
import org.virtuslab.ideprobe.robot.RobotPluginExtension

class ScalaPluginTestSuite
    extends IdeProbeTestSuite
    with ScalaPluginExtension
    with BloopExtension
    with RobotPluginExtension

trait BloopExtension { this: IdeProbeFixture =>

  def startBloopServer(): Unit = {
    val java8 = Jdks.JDK_8.install(ResourceProvider.Default)
    val env = Map("JAVA_HOME" -> java8.toString)
    Shell.async(env, coursierPath.toString, "launch", "bloop", "--", "server")
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

  registerFixtureTransformer { fixture =>
    fixture.withAfterWorkspaceSetup((_, _) => ensureBloopIsNotRunning())
  }

  private val jvmsToKill = Set(
    "bloop.Server",
    "com.martiansoftware.nailgun.NGServer",
    "org.jetbrains.plugins.scala.nailgun.NailgunRunner"
  )

  // we are using different versions of bloop and one test may break other
  private def ensureBloopIsNotRunning(): Unit = {
    Shell.run("jps", "-l").out.linesIterator.map(_.split(" ")).foreach {
      case Array(pid, mainClass) =>
        if (jvmsToKill.contains(mainClass)) {
          Shell.run("kill", "-9", pid)
        }
      case _ => ()
    }
  }
}
