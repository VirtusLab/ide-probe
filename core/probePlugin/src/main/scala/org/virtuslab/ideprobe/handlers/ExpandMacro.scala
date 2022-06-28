package org.virtuslab.ideprobe.handlers

import com.intellij.ide.`macro`.MacroManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import gnu.trove.THashMap

import org.virtuslab.ideprobe.protocol.ExpandMacroData
import org.virtuslab.ideprobe.protocol.FileRef

object ExpandMacro {
  def expand(macroRequest: ExpandMacroData): String = {
    val fileRef = macroRequest.fileRef
    val macroText = macroRequest.macroText

    val project = Projects.resolve(fileRef.project)
    val file = VFS.toVirtualFile(fileRef.path)
    val context = makeDataContext(fileRef, project, file)
    MacroManager.getInstance().expandMacrosInString(macroText, false, context)
  }

  private def makeDataContext(
      fileRef: FileRef,
      project: Project,
      file: VirtualFile
  ) = {
    val data = new THashMap[String, Object]()
    data.put(CommonDataKeys.PROJECT.getName, fileRef.project)
    data.put(CommonDataKeys.VIRTUAL_FILE.getName, file)
    data.put(PlatformDataKeys.PROJECT_FILE_DIRECTORY.getName, project.getBaseDir)
    SimpleDataContext.getSimpleContext(data, null)
  }
}
