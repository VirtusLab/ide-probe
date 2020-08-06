name := "ideprobe"

val scala212 = "2.12.10"
val scala213 = "2.13.1"

skip in publish := true

scalaVersion.in(ThisBuild) := scala213
intellijBuild.in(ThisBuild) := "202.6397.20-EAP-SNAPSHOT"
licenses.in(ThisBuild) := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
organization.in(ThisBuild) := "org.virtuslab.ideprobe"
homepage.in(ThisBuild) := Some(url("https://github.com/VirtusLab/ide-probe"))
developers.in(ThisBuild) := List(
  Developer(
    "lwawrzyk",
    "Łukasz Wawrzyk",
    "lwawrzyk@virtuslab.com",
    url("https://github.com/lukaszwawrzyk")
  ),
  Developer(
    "marek1840",
    "Marek Żarnowski",
    "mzarnowski@virtuslab.com",
    url("https://github.com/marek1840")
  ),
  Developer(
    "tpasternak",
    "Tomasz Pasternak",
    "tpasternak@virtuslab.com",
    url("https://github.com/tpasternak")
  )
)

crossScalaVersions := Nil
sonatypeProfileName := "org.virtuslab"

resolvers.in(ThisBuild) += MavenRepository(
  "jetbrains-3rd",
  "https://jetbrains.bintray.com/intellij-third-party-dependencies"
)

import IdeaPluginAdapter._

/**
 * By default, the sbt-idea-plugin gets applied to all of the projects.
 * We want it only in the plugin projects, so we need to disable it here
 * as well as for each created project separately.
 */
disableIdeaPluginDevelopment()

lazy val ci = project("ci", "ci", publish = false)
  .settings(
    CI.generateScripts := {
      for {
        (group, projects) <- CI.groupedProjects().value.toList
        version <- crossScalaVersions.value
      } yield CI.generateTestScript(group, projects, version)
    }
  )

/**
 * Not a [[module]] so it can be bundled with the idea plugin
 * (doesn't work when used disableIdeaPluginDevelopment on a project)
 */
lazy val api = project("api", "api", publish = true)
  .settings(
    libraryDependencies ++= Dependencies.pureConfig,
    libraryDependencies += Dependencies.gson
  )

lazy val driver = module("driver", "driver/sources")
  .dependsOn(api)
  .enablePlugins(BuildInfoPlugin)
  .usesIdeaPlugin(probePlugin)
  .settings(
    libraryDependencies += Dependencies.nuProcess,
    libraryDependencies += Dependencies.remoteRobot,
    libraryDependencies += Dependencies.remoteRobotFixtures,
    buildInfoKeys := Seq[BuildInfoKey](version, intellijBuild, "robotVersion" -> Dependencies.remoteRobot.revision),
    buildInfoPackage := "org.virtuslab.ideprobe"
  )

lazy val driverTests = testModule("driver-tests", "driver/tests")
  .dependsOn(driver, junitDriver, api % "compile->compile;test->test")
  .usesIdeaPlugin(driverTestPlugin)

lazy val probePlugin = ideaPluginModule("probe-plugin", "probePlugin", publish = true)
  .dependsOn(api)
  .settings(
    intellijPluginName := "ideprobe"
  )

lazy val driverTestPlugin = ideaPluginModule("probe-test-plugin", "driver/test-plugin")
  .settings(
    intellijPluginName := "driver-test-plugin"
  )

lazy val junitDriver = module("junit-driver", "driver/bindings/junit")
  .dependsOn(driver, api % "compile->compile;test->test")
  .settings(
    libraryDependencies ++= Dependencies.junit
  )

lazy val scalaTests = testModule("scala-tests", "extensions/scala/tests")
  .dependsOn(junitDriver)

lazy val examples = testModule("examples", "examples")
  .dependsOn(junitDriver)
  .settings(libraryDependencies += Dependencies.junitJupiterParams)

val commonSettings = Seq(
  libraryDependencies ++= Dependencies.junit,
  test in assembly := {},
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp.filter(file => file.data.toString.contains(".ideprobePluginIC"))
  }
)

def project(id: String, path: String, publish: Boolean): Project = {
  Project(id, sbt.file(path))
    .settings(
      skip in Keys.publish := !publish,
      libraryDependencies ++= Dependencies.junit,
      crossScalaVersions := List(scala213, scala212),
      test in assembly := {},
      assemblyExcludedJars in assembly := {
        val cp = (fullClasspath in assembly).value
        cp.filter(file => file.data.toString.contains(".ideprobePluginIC"))
      }
    )
}

def module(id: String, path: String): Project = {
  project(id, path, publish = true).disableIdeaPluginDevelopment
    .settings(
      skip in Keys.publish := false
    )
}

def testModule(id: String, path: String): Project = {
  project(id, path, publish = false).disableIdeaPluginDevelopment
    .settings(
      Test / publishArtifact := true
    )
}

def ideaPluginModule(id: String, path: String, publish: Boolean = false): Project = {
  project(id, path, publish).enableIdeaPluginDevelopment
    .settings(
      packageMethod := PackagingMethod.Standalone(),
      intellijPlugins ++= Seq(
        "com.intellij.java".toPlugin,
        "JUnit".toPlugin
      )
    )
}
