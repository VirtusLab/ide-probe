package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.{PsiDirectory, PsiElement, PsiFile, PsiManager, PsiPackage}
import org.virtuslab.ideprobe.protocol.FileRef
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.Reference
import scala.collection.mutable

object PSI extends IntelliJApi {
  def findPackage(project: Project, packageName: String): Option[PsiPackage] = read {
    Option(new PsiPackageImpl(manager(project), packageName))
  }

  def findDirectory(project: Project, file: VirtualFile): Option[PsiDirectory] = read {
    Option(manager(project).findDirectory(file))
  }

  def resolve(ref: FileRef): PsiFile = {
    val project = Projects.resolve(ref.project)
    val file = VFS.resolve(ref)
    read { manager(project).findFile(file) }
  }

  def references(file: FileRef): Seq[Reference] = {
    references(resolve(file))
  }

  def references(root: PsiElement): Seq[Reference] = read {
    val references = mutable.Buffer[Reference]()
    val elements = mutable.Stack[PsiElement](root)
    while (elements.nonEmpty) {
      val element = elements.pop()
      elements.pushAll(element.getChildren)
      element.getReferences.foreach { reference =>
        val text = reference.getCanonicalText
        Option(reference.resolve())
          .flatMap(toTarget)
          .map(Reference(text, _))
          .foreach(references += _)
      }
    }

    references.toSet.toList
  }

  private def manager(project: Project) = {
    PsiManager.getInstance(project)
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
