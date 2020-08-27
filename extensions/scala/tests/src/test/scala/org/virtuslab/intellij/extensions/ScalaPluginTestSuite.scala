package org.virtuslab.intellij.extensions

import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.junit4.IdeProbeTestSuite
import org.virtuslab.ideprobe.scala.ScalaPluginExtension

class ScalaPluginTestSuite
  extends IdeProbeTestSuite
    with ScalaPluginExtension
    with BloopExtension

trait BloopExtension { this: IdeProbeFixture =>

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
