package org.virtuslab.ideprobe.handlers

import com.intellij.codeInspection
import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.QuickFix
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.virtuslab.ideprobe.protocol.InspectionRunParams
import org.virtuslab.ideprobe.protocol.InspectionRunResult
import org.virtuslab.ideprobe.protocol.ProblemDescriptor
import org.virtuslab.ideprobe.protocol.RunFixesSpec

object Inspections extends IntelliJApi {

  def runLocal(params: InspectionRunParams): InspectionRunResult = BackgroundTasks.withAwaitNone {
    val localInspectionEP = LocalInspectionEP.LOCAL_INSPECTION.getExtensions
      .find(_.implementationClass == params.className)
      .getOrElse(error(s"Could not find extension with class ${params.className}"))
    val classLoader = localInspectionEP.getPluginDescriptor.getPluginClassLoader
    val inspectionClass = classLoader.loadClass(params.className)
    val inspection = inspectionClass.getDeclaredConstructor().newInstance().asInstanceOf[LocalInspectionTool]

    val psiFile = PSI.resolve(params.targetFile)
    val project = psiFile.getProject

    val descriptors = read { inspection.checkFile(psiFile, InspectionManager.getInstance(project), false) }

    def runFixes(
        descriptor: codeInspection.ProblemDescriptor,
        predicate: QuickFix[CommonProblemDescriptor] => Boolean = _ => true
    ): Unit = {
      val fixes = descriptor.getFixes.toList.asInstanceOf[List[QuickFix[CommonProblemDescriptor]]]
      fixes.filter(predicate).foreach { fix =>
        runOnUISync { write { fix.applyFix(project, descriptor.asInstanceOf[CommonProblemDescriptor]) } }
      }
    }

    params.runFixes match {
      case RunFixesSpec.None            => ()
      case RunFixesSpec.All             => descriptors.foreach(runFixes(_))
      case RunFixesSpec.Specific(fixes) => descriptors.foreach(runFixes(_, fix => fixes.contains(fix.getName)))
    }

    InspectionRunResult(
      descriptors.map(desc =>
        read {
          ProblemDescriptor(
            desc.getDescriptionTemplate,
            desc.getLineNumber,
            desc.getPsiElement.getText,
            desc.getFixes.map(_.getName)
          )
        }
      )
    )
  }

}
