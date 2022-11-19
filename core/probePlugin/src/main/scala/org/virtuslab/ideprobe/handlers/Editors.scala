package org.virtuslab.ideprobe.handlers

import java.nio.file.Path
import java.nio.file.Paths

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor

import org.virtuslab.ideprobe.handlers.Projects.resolve
import org.virtuslab.ideprobe.protocol.FileRef
import org.virtuslab.ideprobe.protocol.ProjectRef

object Editors extends IntelliJApi {

  def all(projectRef: ProjectRef): Seq[Path] = runOnUISync {
    editorManager(projectRef).getOpenFiles.toIndexedSeq.map(p => Paths.get(p.getPath))
  }

  def open(fileRef: FileRef): Unit =
    runOnUISync {
      val vFile = VFS.toVirtualFile(fileRef.path, refresh = true)
      val project = resolve(fileRef.project)
      new OpenFileDescriptor(project, vFile).navigate(true)
    }

  def close(fileRef: FileRef): Unit =
    runOnUISync {
      val vFile = VFS.toVirtualFile(fileRef.path, refresh = true)
      editorManager(fileRef.project).closeFile(vFile)
    }

  def goToLineColumn(projectRef: ProjectRef, line: Int, column: Int): Unit = {
    runOnUISync {
      val editor = editorManager(projectRef).getSelectedTextEditor
      val newPosition = new LogicalPosition(line - 1, column - 1)
      editor.getCaretModel.moveToLogicalPosition(newPosition)
    }
  }

  private def editorManager(projectRef: ProjectRef) = {
    FileEditorManager.getInstance(resolve(projectRef))
  }

}
