package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.handlers.Projects.resolve
import org.virtuslab.ideprobe.protocol.{FileRef, ProjectRef}

object Editors extends IntelliJApi {

  def all(projectRef: ProjectRef): Seq[Path] = runOnUISync {
    editorManager(projectRef).getOpenFiles.map(p => Paths.get(p.getPath))
  }

  def open(fileRef: FileRef): Unit =
    runOnUISync {
      val vFile = VFS.toVirtualFile(fileRef.path, refresh = true)
      val project = resolve(fileRef.project)
      new OpenFileDescriptor(project, vFile).navigate(true)
    }

  def goToLineColumn(projectRef: ProjectRef, line: Int, column: Int): Unit = {
    runOnUISync {
      val editor = editorManager(projectRef).getSelectedTextEditor
      val newPosition = new LogicalPosition(line-1, column-1)
      editor.getCaretModel.moveToLogicalPosition(newPosition)
    }
  }

  private def editorManager(projectRef: ProjectRef) = {
    FileEditorManager.getInstance(resolve(projectRef))
  }

}
