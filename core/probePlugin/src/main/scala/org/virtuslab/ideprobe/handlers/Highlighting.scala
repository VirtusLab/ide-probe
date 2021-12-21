package org.virtuslab.ideprobe.handlers

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.codeInsight.daemon.impl.{DaemonCodeAnalyzerEx, DaemonCodeAnalyzerImpl, HighlightInfo}
import com.intellij.openapi.editor.{Document, Editor}
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.impl.text.{AsyncEditorLoader, TextEditorProvider}
import com.intellij.openapi.fileEditor.{FileEditor, FileEditorManager, OpenFileDescriptor, TextEditor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.{PsiDocumentManager, PsiFile}
import com.intellij.util.ui.UIUtil
import java.util
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.locks.LockSupport
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol
import org.virtuslab.ideprobe.protocol.{FileRef}

object Highlighting extends IntelliJApi {

  def infos(fileRef: FileRef): Seq[protocol.HighlightInfo] = {
    val psiFile = PSI.resolve(fileRef)
    val document = PSI.getDocument(psiFile)

    runHighlighting(psiFile)
    val highlights = collectHighlightInfos(psiFile, document)

    formatResult(highlights, fileRef, document)
  }

  private def formatResult(highlights: List[HighlightInfo], fileRef: FileRef, document: Document) = {
    highlights.filter(_.getDescription != null).map { info =>
      protocol.HighlightInfo(
        fileRef.path,
        document.getLineNumber(info.getStartOffset) + 1,
        info.getStartOffset,
        info.getEndOffset,
        protocol.HighlightInfo.Severity.withName(info.getSeverity.toString),
        info.getDescription
      )
    }
  }

  private def collectHighlightInfos(
      psiFile: PsiFile,
      document: Document
  ) = read {
    val project = psiFile.getProject
    val basic = DaemonCodeAnalyzerImpl.getHighlights(document, null, project)
    val fileLevel = {
      val analyzer = DaemonCodeAnalyzerEx.getInstanceEx(project).asInstanceOf[DaemonCodeAnalyzerImpl]
      analyzer.getFileLevelHighlights(project, psiFile)
    }
    (basic.asScala ++ fileLevel.asScala).toList
  }

  private def runHighlighting(psiFile: PsiFile): Unit = {
    val project = psiFile.getProject

    var editor: Editor = null
    val latch = new CountDownLatch(1)

    val connection = project.getMessageBus.connect()
    connection.subscribe(
      DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
      new DaemonListener() {
        override def daemonFinished(fileEditors: util.Collection[_ <: FileEditor]): Unit = {
          if (editor != null && finishedForEditor(editor, fileEditors)) {
            latch.countDown()
          }
        }

        private def finishedForEditor(editor: Editor, fileEditors: util.Collection[_ <: FileEditor]) = {
          fileEditors.asScala.exists {
            case e: TextEditor => e.getEditor == editor
            case _             => false
          }
        }
      }
    )

    editor = runOnUISync { createEditor(project, psiFile.getVirtualFile) }

    val waitingSuccess = latch.await(1, TimeUnit.MINUTES)
    connection.disconnect()
    if (!waitingSuccess) {
      error(s"Failed to collect highlight info for ${psiFile.getVirtualFile.getPath}")
    }
  }

  // adapted from intellij-community tests
  protected def createEditor(project: Project, file: VirtualFile): Editor = {
    val instance = FileEditorManager.getInstance(project)
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val editor = instance.openTextEditor(new OpenFileDescriptor(project, file), false)

    if (EditorUtil.isRealFileEditor(editor)) {
      UIUtil.dispatchAllInvocationEvents()
      while (!AsyncEditorLoader.isEditorLoaded(editor)) {
        LockSupport.parkNanos(100000000L)
        UIUtil.dispatchAllInvocationEvents()
      }
    }

    editor
  }

}
