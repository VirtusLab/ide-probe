package org.virtuslab.ideprobe.scala

import org.virtuslab.ideprobe.{IdeProbeFixture, Shell}

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
