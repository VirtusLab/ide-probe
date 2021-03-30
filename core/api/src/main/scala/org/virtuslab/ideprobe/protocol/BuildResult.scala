package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

case class BuildMessage(file: Option[String], content: String)

case class BuildResult(results: Seq[BuildStepResult]) {
  def assertSuccess(): Unit = if (hasErrors || isAborted) throw new AssertionError(s"Build did not succeed: $this")

  def hasErrors: Boolean = errors.nonEmpty

  def isAborted: Boolean = merge(_.isAborted)(_ || _)

  def errors: Seq[BuildMessage] = merge(_.errors)(_ ++ _)

  def warnings: Seq[BuildMessage] = merge(_.warnings)(_ ++ _)

  def infos: Seq[BuildMessage] = merge(_.infos)(_ ++ _)

  def stats: Seq[BuildMessage] = merge(_.stats)(_ ++ _)

  private def merge[A](get: BuildStepResult => A)(merge: (A, A) => A): A = {
    results.map(get).reduce(merge)
  }
}

case class BuildStepResult(
    isAborted: Boolean,
    errors: Seq[BuildMessage],
    warnings: Seq[BuildMessage],
    infos: Seq[BuildMessage],
    stats: Seq[BuildMessage]
) {
  def hasErrors: Boolean = errors.nonEmpty
}

case class BuildParams(scope: BuildScope, rebuild: Boolean)

case class BuildScope(project: ProjectRef, modules: Seq[String], files: Seq[String])

object BuildScope {
  def project: BuildScope = {
    BuildScope(project = ProjectRef.Default, modules = Nil, files = Nil)
  }

  def project(project: ProjectRef): BuildScope = {
    BuildScope(project = project, modules = Nil, files = Nil)
  }

  def modules(project: ProjectRef, modules: String*): BuildScope = {
    BuildScope(project = project, modules = modules, files = Nil)
  }

  def files(project: ProjectRef, paths: Path*): BuildScope = {
    BuildScope(project = project, modules = Nil, files = paths.map(_.toString))
  }
}
