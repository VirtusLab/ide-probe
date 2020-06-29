name := "ideprobe"

organization.in(ThisBuild) := "com.virtuslab"
version.in(ThisBuild) := "0.1"
scalaVersion.in(ThisBuild) := "2.13.1"
intellijBuild.in(ThisBuild) := "202.5792.28-EAP-SNAPSHOT"
licenses.in(ThisBuild) := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
skip in publish := true

import IdeaPluginAdapter._
import IdeaPluginDevelopment._

/**
 * By default, the sbt-idea-plugin gets applied to all of the projects.
 * We want it only in the plugin projects, so we need to disable it here
 * as well as for each created project separately.
 */
disableIdeaPluginDevelopment()

val commonSettings = Seq(
  test in assembly := {},
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp.filter(file => file.data.toString.contains(".ideprobePluginIC"))
  }
)

val pluginSettings = Seq(
  packageMethod := PackagingMethod.Standalone(),
  intellijPlugins ++= Seq(
    "com.intellij.java".toPlugin,
    "JUnit".toPlugin
  )
)

lazy val ci = project.settings(
  CI.generateScripts := {
    CI.groupedProjects().value.toList.map {
      case (group, projects) => CI.generateTestScript(group, projects)
    }
  },
  skip in publish := true
)

lazy val api = project
  .in(file("api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.junit,
    libraryDependencies ++= Dependencies.pureConfig,
    libraryDependencies ++= Seq(
      Dependencies.gson,
      Dependencies.ammonite
    )
  )

lazy val driver = project
  .enablePlugins(BuildInfoPlugin)
  .in(file("driver/sources"))
  .disableIdeaPluginDevelopment
  .dependsOn(api)
  .usesIdeaPlugin(probePlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "driver",
    libraryDependencies ++= Dependencies.junit,
    libraryDependencies ++= Seq(
      Dependencies.scalaParallelCollections,
      Dependencies.nuProcess
    ),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "com.virtuslab.ideprobe"
  )

lazy val driverTests = project
  .in(file("driver/tests"))
  .settings(commonSettings: _*)
  .disableIdeaPluginDevelopment
  .dependsOn(driver, junitDriver, api % "compile->compile;test->test")
  .usesIdeaPlugin(driverTestPlugin)
  .settings(
    libraryDependencies ++= Dependencies.junit,
    name := "driver-tests",
    Test / publishArtifact := true
  )

lazy val driverTestPlugin = ideaPlugin("driver/test-plugin", id = "driverTestPlugin")
  .settings(
    intellijPluginName := "driver-test-plugin",
    name := "probe-test-plugin"
  )

lazy val junitDriver = project
  .in(file("driver/bindings/junit"))
  .settings(commonSettings: _*)
  .disableIdeaPluginDevelopment
  .dependsOn(driver, api % "compile->compile;test->test")
  .settings(
    name := "junit-driver",
    libraryDependencies ++= Dependencies.junit,
    skip in publish := false
  )

lazy val probePlugin = ideaPlugin("probePlugin")
  .dependsOn(api)
  .settings(commonSettings: _*)
  .settings(
    intellijPluginName := "ideprobe",
    name := "probe-plugin"
  )

lazy val scalaTests = project
  .in(file("extensions/scala/tests"))
  .disableIdeaPluginDevelopment
  .dependsOn(junitDriver)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.junit,
    Test / publishArtifact := true,
    name := "scala-tests"
  )

def ideaPlugin(path: String, id: String = null) = {
  val resolvedId = Option(id).getOrElse(path)
  Project(resolvedId, file(path)).enableIdeaPluginDevelopment
    .settings(pluginSettings)
}
