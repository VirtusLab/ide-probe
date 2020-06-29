package com.virtuslab.ideprobe.ide.intellij

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.virtuslab.ideprobe.OS
import com.virtuslab.ideprobe.ProbeDriver
import com.virtuslab.ideprobe.Shell
import com.zaxxer.nuprocess.NuProcess

final class RunningIde(val launcher: NuProcess, idePID: Long, val probe: ProbeDriver) {

  private val shutdownDone: AtomicBoolean = new AtomicBoolean(false)

  private def runOnce(fn: => Unit) = {
    if (shutdownDone.compareAndSet(false, true)) {
      fn
    }
  }

  def shutdown(): Unit = runOnce {
    try {
      probe.shutdown()
    } finally {
      val launcherPID = launcher.getPID

      launcher.destroy(true)
      val exitCode = launcher.waitFor(30, TimeUnit.SECONDS) // destroy might not work immediately
      if (exitCode == Integer.MIN_VALUE) {
        println("Could't terminate the IDE within the usual timeout. Using system command to terminate.")
      }

      if (launcherPID != idePID) {
        // TODO replace with ProcessHandler when java 9 is supported
        val command = OS.Current match {
          case OS.Unix | OS.Mac => Array("kill", "-9", idePID.toString)
          case OS.Windows       => Array("taskkill", "/F", "/pid", idePID.toString)
        }

        val result = Shell.run(command: _*)
        if (result.exitCode == 0) {
          println("IDE terminated")
        } else if (!result.err.contains("No such process")) {
          println("Couldn't terminate the IDE due to: " + result.err)
        }
      }
    }
  }
}
