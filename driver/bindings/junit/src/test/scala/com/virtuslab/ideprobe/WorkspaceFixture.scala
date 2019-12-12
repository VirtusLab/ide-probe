package com.virtuslab.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import com.virtuslab.ideprobe.Extensions._

trait WorkspaceFixture {

  protected def withWorkspace(block: Path => Unit): Unit = {
    val workspace = Files.createTempDirectory("intellij-rule-workspace")
    try block(workspace)
    finally workspace.delete()
  }

}
