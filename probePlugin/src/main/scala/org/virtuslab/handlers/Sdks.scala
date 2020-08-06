package org.virtuslab.handlers

import java.nio.file.Paths

import org.virtuslab.ideprobe.protocol
import com.intellij.openapi.projectRoots.Sdk

object Sdks {
  def convert(sdk: Sdk): protocol.Sdk = {
    protocol.Sdk(
      sdk.getName,
      sdk.getSdkType.toString,
      Option(sdk.getVersionString),
      Option(sdk.getHomePath).map(Paths.get(_))
    )
  }
}
