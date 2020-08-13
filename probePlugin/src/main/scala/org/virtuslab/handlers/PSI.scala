package org.virtuslab.handlers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.virtuslab.ideprobe.protocol.FileRef
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.Reference
import scala.collection.mutable

object PSI extends IntelliJApi {
  def resolve(ref: FileRef): PsiFile = {
    val project = Projects.resolve(ref.project)
    val file = VFS.resolve(ref)
    read { PsiManager.getInstance(project).findFile(file) }
  }

  def references(file: FileRef): Seq[Reference] = {
    references(resolve(file))
  }

  def references(root: PsiElement): Seq[Reference] = {
    val references = mutable.Buffer[Reference]()
    val elements = mutable.Stack[PsiElement](root)
    while (elements.nonEmpty) {
      val element = elements.pop()
      elements.pushAll(read { element.getChildren })
      element.getReferences.foreach { reference =>
        val text = read { reference.getCanonicalText }
        Option(read { reference.resolve() })
          .flatMap(toTarget)
          .map(Reference(text, _))
          .foreach(references += _)
      }
    }

    references.toSet.toList
  }

  private def toTarget(element: PsiElement): Option[Reference.Target] = {
    Option(element.getProject).map(_.getName).map(ProjectRef(_)).flatMap { project =>
      element match {
        case file: PsiFile =>
          val path = VFS.toPath(file.getVirtualFile)
          val ref = Reference.Target.File(FileRef(path, project))
          Some(ref)
        case _ =>
          None
      }
    }
  }
}
