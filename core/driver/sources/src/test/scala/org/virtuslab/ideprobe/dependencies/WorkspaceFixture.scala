package org.virtuslab.ideprobe.dependencies

import java.nio.file.Files
import java.nio.file.Path

import org.virtuslab.ideprobe.Extensions._

trait WorkspaceFixture {

  protected def withWorkspace(block: Path => Unit): Unit = {
    val workspace = Files.createTempDirectory("intellij-rule-workspace")
    try block(workspace)
    finally workspace.delete()
  }

}
