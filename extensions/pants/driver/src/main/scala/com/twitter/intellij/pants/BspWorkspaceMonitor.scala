package com.twitter.intellij.pants

import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions.PathExtension

object BspWorkspaceMonitor {
  private var initialBspProjects = Map.empty[Path, Seq[Path]]

  Runtime.getRuntime.addShutdownHook(new Thread(() => cleanup()))

  def register(workspace: Path): Unit = {
    initialBspProjects.get(workspace) match {
      case Some(initialProjects) =>
        cleanup(workspace, initialProjects)
      case None =>
        val bspProjects = workspace.resolveSibling("bsp-projects")
        if (bspProjects.isDirectory) {
          initialBspProjects += (workspace -> bspProjects.directChildren())
        } else {
          initialBspProjects += (workspace -> Nil)
        }
    }
  }

  def cleanup(): Unit = {
    initialBspProjects.foreach {
      case (workspace, initialProjects) =>
        cleanup(workspace, initialProjects)
    }
  }

  private def cleanup(
    workspace: Path,
    initialProjects: Seq[Path]
  ): Unit = {
    val bspProjects = workspace.resolveSibling("bsp-projects")
    if (bspProjects.isDirectory) {
      bspProjects.directChildren().filterNot(initialProjects.contains).foreach(_.delete())
    }
  }
}
