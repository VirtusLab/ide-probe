package org.virtuslab.ideprobe.handlers

import java.nio.file.Paths

import com.intellij.openapi.projectRoots.Sdk

import org.virtuslab.ideprobe.protocol

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
