name := "ideprobe"

val scala212 = "2.12.10"
val scala213 = "2.13.1"
val crossScalaVersions = List(scala212, scala213)

skip in publish := true

scalaVersion.in(ThisBuild) := scala213
intellijBuild.in(ThisBuild) := "202.6948.69"
// provide intellij version in case of the release version
val intellijVersion = None
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

sonatypeProfileName := "org.virtuslab"

resolvers.in(ThisBuild) += Resolver.jcenterRepo

import IdeaPluginAdapter._
import IdeaPluginDevelopment.packageArtifactZipFilter

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
        (group, projects) <- CI.groupedProjects(crossScalaVersions).value.toList
        version <- crossScalaVersions
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
  .cross

lazy val api212 = api(scala212)

lazy val api213 = api(scala213)

lazy val driver = module("driver", "driver/sources")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies += Dependencies.nuProcess,
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      intellijBuild,
      "intellijVersion" -> intellijVersion,
      "robotVersion" -> Dependencies.remoteRobot.revision
    ),
    buildInfoPackage := "org.virtuslab.ideprobe"
  )
  .cross
  .dependsOn(api)

lazy val driver212 = driver(scala212)
  .usesIdeaPlugin(probePlugin212)

lazy val driver213 = driver(scala213)
  .usesIdeaPlugin(probePlugin213)

lazy val robotDriver = module("robot-driver", "extensions/robot/driver")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    resolvers += MavenRepository(
      "jetbrains-3rd",
      "https://jetbrains.bintray.com/intellij-third-party-dependencies"
    ),
    libraryDependencies += Dependencies.remoteRobot,
    libraryDependencies += Dependencies.remoteRobotFixtures,
    buildInfoKeys := Seq[BuildInfoKey](
      "robotVersion" -> Dependencies.remoteRobot.revision
    ),
    buildInfoPackage := "org.virtuslab.ideprobe.robot",
    name := "robot-driver"
  )
  .cross
  .dependsOn(driver)

lazy val robotDriver212 = robotDriver(scala212)

lazy val robotDriver213 = robotDriver(scala213)

lazy val driverTests = testModule("driver-tests", "driver/tests").cross
  .dependsOn(junitDriver, robotDriver, api % "compile->compile;test->test")

lazy val driverTests212 = driverTests(scala212)
  .usesIdeaPlugin(driverTestPlugin212)

lazy val driverTests213 = driverTests(scala213)
  .usesIdeaPlugin(driverTestPlugin213)

lazy val probePlugin = ideaPluginModule("probe-plugin", "probePlugin", publish = true)
  .settings(intellijPluginName := "ideprobe")
  .cross
  .dependsOn(api)

lazy val probePlugin212 = probePlugin(scala212)

lazy val probePlugin213 = probePlugin(scala213)

lazy val driverTestPlugin = ideaPluginModule("probe-test-plugin", "driver/test-plugin")
  .settings(intellijPluginName := "driver-test-plugin")
  .cross

lazy val driverTestPlugin212 = driverTestPlugin(scala212)

lazy val driverTestPlugin213 = driverTestPlugin(scala213)

lazy val junitDriver = module("junit-driver", "driver/bindings/junit")
  .settings(libraryDependencies ++= Dependencies.junit)
  .cross
  .dependsOn(driver, api % "compile->compile;test->test")

lazy val junitDriver212 = junitDriver(scala212)

lazy val junitDriver213 = junitDriver(scala213)

lazy val scalaProbeApi = project(id = "scala-probe-api", path = "extensions/scala/api", publish = true).cross
  .dependsOn(api)

lazy val scalaProbeApi212 = scalaProbeApi(scala212)

lazy val scalaProbeApi213 = scalaProbeApi(scala213)

lazy val scalaProbePlugin =
  ideaPluginModule(id = "scala-probe-plugin", path = "extensions/scala/probePlugin", publish = true)
    .settings(
      intellijPluginName := "ideprobe-scala",
      packageArtifactZipFilter := { file: File =>
        // We want only this main jar to be packaged, all the library dependencies
        // are already in the probePlugin which will be available in runtime as we
        // depend on it in plugin.xml.
        // The packaging plugin is created to support one plugin per build, so there
        // seems to be no way to prevent including probePlugin.jar in the dist reasonable way.
        file.getName.contains("scala-probe-plugin")
      },
      name := "scala-probe-plugin"
    )
    .cross
    .dependsOn(probePlugin, scalaProbeApi)

lazy val scalaProbePlugin212 = scalaProbePlugin(scala212).settings(
  intellijPlugins += "org.intellij.scala:2020.2.49".toPlugin
)

lazy val scalaProbePlugin213 = scalaProbePlugin(scala213).settings(
  intellijPlugins += "org.intellij.scala:2020.3.369:nightly".toPlugin
)

lazy val scalaProbeDriver =
  module(id = "scala-probe-driver", path = "extensions/scala/driver")
    .settings(name := "scala-probe-driver")
    .cross
    .dependsOn(scalaProbeApi, driver)

lazy val scalaProbeDriver212 = scalaProbeDriver(scala212)
  .usesIdeaPlugin(scalaProbePlugin212)

lazy val scalaProbeDriver213 = scalaProbeDriver(scala213)
  .usesIdeaPlugin(scalaProbePlugin213)

lazy val scalaTests = testModule("scala-tests", "extensions/scala/tests").cross
  .dependsOn(junitDriver, robotDriver, scalaProbeDriver)

lazy val scalaTests213 = scalaTests(scala213)
  .usesIdeaPlugin(scalaProbePlugin213)

lazy val examples = testModule("examples", "examples")
  .cross
  .dependsOn(driver, robotDriver, scalaProbeDriver)

lazy val examples213 = examples(scala213)

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
