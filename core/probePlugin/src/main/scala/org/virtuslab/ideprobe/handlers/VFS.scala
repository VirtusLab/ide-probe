package org.virtuslab.ideprobe.handlers

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

import scala.util.Try

import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

import org.virtuslab.ideprobe.protocol.FileRef

object VFS extends IntelliJApi {

  def resolve(ref: FileRef): VirtualFile = {
    toVirtualFile(ref.path)
  }

  def syncAll(): Unit = {
    runOnUISync {
      write {
        FileDocumentManager.getInstance.saveAllDocuments()
        SaveAndSyncHandler.getInstance.refreshOpenFiles()
        VirtualFileManager.getInstance.refreshWithoutFileWatcher(false)
      }
    }
  }

  def toVirtualFile(path: Path, refresh: Boolean = false): VirtualFile = {
    if (refresh) {
      LocalFileSystem.getInstance.refreshAndFindFileByNioFile(path)
    } else {
      LocalFileSystem.getInstance.findFileByNioFile(path)
    }
  }

  def toPath(virtualFile: VirtualFile): Path = {
    Try(URI.create(virtualFile.getPath)).map(Paths.get).getOrElse(Paths.get(virtualFile.getPath))
  }
}
