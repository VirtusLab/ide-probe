import java.nio.file.Paths

import sbt.Keys.loadedBuild
import sbt.{Def, ProjectRef, _}

object CI {
  private val excluded = Set("ci", "ide-probe", "ideprobe", "probe", "examples")
  lazy val generateScripts = taskKey[Seq[File]]("Generate CI scripts")

  private def scalaVersionToModulePostfix(scalaVersion: String): String =
    scalaVersion.split("\\.").dropRight(1).mkString("_")

  def groupedProjects(scalaVersions: List[String]): Def.Initialize[Task[Map[String, Seq[ProjectRef]]]] = Def.task {
    val extensionDir = Paths.get(loadedBuild.value.root).resolve("extensions")
    val excludedCross = for {
      version <- scalaVersions
      module <- excluded
    } yield s"${module}_${scalaVersionToModulePostfix(version)}"

    loadedBuild.value.allProjectRefs
      .filterNot { case (_, project) => excludedCross.contains(project.id) }
      .groupBy { case (_, project) =>
        val projectPath = project.base.toPath
        if (projectPath.startsWith(extensionDir)) extensionDir.relativize(projectPath).getName(0).toString
        else "probe"
      }
      .mapValues(_.map(_._1))
  }

  def generateTestScript(group: String, projects: Seq[ProjectRef], scalaVersion: String): sbt.File = {
    val script = file(s"ci/tests/$scalaVersion/test-$group")
    val arguments = projects
      .filter(_.project.contains(scalaVersionToModulePostfix(scalaVersion)))
      .map(ref => s"; ${ref.project} / test")
      .mkString
    val content =
      s"""|#!/bin/sh
          |
          |export IDEPROBE_DISPLAY=xvfb
          |export JAVA_HOME=/usr/local/openjdk-11
          |export IDEPROBE_SCREENSHOTS_DIR=/tmp/ideprobe/screenshots
          |
          |sbt "; clean $arguments"
          |""".stripMargin
    IO.write(script, content)
    println(s"Generated $script")
    script
  }
}
