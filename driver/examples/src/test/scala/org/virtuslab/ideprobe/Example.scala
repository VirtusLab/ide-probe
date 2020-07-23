package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import org.junit.Test
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}

import scala.concurrent.ExecutionContext

class Example {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  private val dokka = "https://github.com/Kotlin/dokka"
  private val cats = "https://github.com/typelevel/cats"
  private val fixture = IntelliJFixture(
    version = IntelliJVersion("202.5792.28-EAP-SNAPSHOT"),
    plugins = List(Plugin("org.intellij.scala", "2020.2.584", Some("nightly")))
  )

  @Test
  def testImportProjectWithoutPlugins: Unit =
    fixture.copy(workspaceTemplate = WorkspaceTemplate.FromGit(dokka, None)).run { intelliJ =>
      intelliJ.probe.preconfigureJDK()
      val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
      val project = intelliJ.probe.projectModel(projectRef)
      val module = "ws"

      assert(project.modules.map(_.name) contains module)
    }

  @Test
  def testImportProjectWithPlugin: Unit = fixture.copy(workspaceTemplate = WorkspaceTemplate.FromGit(cats, None)).run {
    intelliJ =>
      intelliJ.probe.preconfigureJDK()
      val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
      val project = intelliJ.probe.projectModel(projectRef)
      val module = "cats-build"

      assert(project.modules.map(_.name) contains module)
  }
}
