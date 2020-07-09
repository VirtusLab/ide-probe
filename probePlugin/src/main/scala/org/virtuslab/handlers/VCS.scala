package org.virtuslab.handlers

import java.nio.file.Paths

import com.intellij.openapi.vcs.ProjectLevelVcsManager
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol

object VCS {
  def roots(projectRef: ProjectRef): Seq[protocol.VcsRoot] = {
    val project = Projects.resolve(projectRef)
    val vcsManager = ProjectLevelVcsManager.getInstance(project)
    val currentVcsRoots = vcsManager.getAllVcsRoots
    currentVcsRoots.map(root => protocol.VcsRoot(root.getVcs.toString, VFS.toPath(root.getPath)))
  }
}
