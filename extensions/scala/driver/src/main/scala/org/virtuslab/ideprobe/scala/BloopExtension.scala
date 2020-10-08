package org.virtuslab.ideprobe.scala

import java.net.URL
import java.nio.file.{Files, Path, Paths}
import org.virtuslab.ideprobe.{IdeProbeFixture, Shell}

trait BloopExtension { this: IdeProbeFixture =>

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
