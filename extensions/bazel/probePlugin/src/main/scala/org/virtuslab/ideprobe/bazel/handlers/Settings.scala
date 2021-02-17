package org.virtuslab.ideprobe.bazel.handlers

import com.google.idea.blaze.base.settings.BlazeUserSettings
import java.nio.file.Path
import org.virtuslab.ideprobe.handlers.IntelliJApi

object Settings extends IntelliJApi {

  def setupBazelExecutable(bazelPath: Path): Unit = {
    runOnUISync {
      write {
        BlazeUserSettings.getInstance.setBazelBinaryPath(bazelPath.toString)
      }
    }
  }

}
