name := "ideprobe"

val scala212 = "2.12.15"
val scala213 = "2.13.8"
val crossScalaVersions = List(scala212, scala213)

(publish / skip) := true

(ThisBuild / scalaVersion) := scala213
(ThisBuild / intellijBuild) := "221.5591.52"
(ThisBuild / bundleScalaLibrary) := true
// provide intellij version in case of the release version
val intellijVersion = None
(ThisBuild / licenses) := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
(ThisBuild / organization) := "org.virtuslab.ideprobe"
(ThisBuild / homepage) := Some(url("https://github.com/VirtusLab/ide-probe"))
(ThisBuild / developers) := List(
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

(ThisBuild / resolvers) += Resolver.jcenterRepo

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
lazy val api = project("api", "core/api", publish = true)
  .settings(
    libraryDependencies ++= Dependencies.pureConfig,
    libraryDependencies += Dependencies.gson
  )
  .cross

lazy val api213 = api(scala213)

lazy val driver = module("driver", "core/driver/sources")
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

lazy val driver213 = driver(scala213)
  .usesIdeaPlugins(probePlugin213, probePlugin212)

lazy val robotDriver = module("robot-driver", "extensions/robot/driver")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    resolvers += MavenRepository(
      "jetbrains-3rd",
      "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies"
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

lazy val robotDriver213 = robotDriver(scala213)

lazy val driverTests = testModule("driver-tests", "core/driver/tests").cross
  .dependsOn(junitDriver, robotDriver, api % "compile->compile;test->test")

lazy val driverTests213 = driverTests(scala213)
  .usesIdeaPlugins(driverTestPlugin213, driverTestPlugin212)

lazy val probePlugin = ideaPluginModule("probe-plugin", "core/probePlugin", publish = true)
  .settings(intellijPluginName := "ideprobe")
  .cross
  .dependsOn(api)

lazy val probePlugin213 = probePlugin(scala213)
  .settings(libraryDependencies ++= Dependencies.scalaLib(scala213))

lazy val driverTestPlugin = ideaPluginModule("probe-test-plugin", "core/driver/test-plugin")
  .settings(intellijPluginName := "driver-test-plugin")
  .cross

lazy val driverTestPlugin213 = driverTestPlugin(scala213)

lazy val junitDriver = module("junit-driver", "core/driver/bindings/junit")
  .settings(
    libraryDependencies ++= Dependencies.junit,
    libraryDependencies ++= Dependencies.junitCompile
  )
  .cross
  .dependsOn(driver, api % "compile->compile;test->test")

lazy val junitDriver213 = junitDriver(scala213)

// scala extension
lazy val scalaProbeApi =
  project(id = "scala-probe-api", path = "extensions/scala/api", publish = true).cross
    .dependsOn(api)

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

lazy val scalaProbePlugin213 = scalaProbePlugin(scala213).settings(
  intellijPlugins += "org.intellij.scala:2022.1.14".toPlugin
)

lazy val scalaProbeDriver =
  module(id = "scala-probe-driver", path = "extensions/scala/driver")
    .settings(name := "scala-probe-driver")
    .cross
    .dependsOn(scalaProbeApi, driver)

lazy val scalaProbeDriver213 = scalaProbeDriver(scala213)
  .usesIdeaPlugins(scalaProbePlugin213, scalaProbePlugin212)

lazy val scalaTests = testModule("scala-tests", "extensions/scala/tests").cross
  .dependsOn(junitDriver, robotDriver, scalaProbeDriver)

lazy val scalaTests213 = scalaTests(scala213)
  .usesIdeaPlugin(scalaProbePlugin213)

// pants extension
lazy val pantsProbeApi =
  project(id = "pants-probe-api", path = "extensions/pants/api", publish = true).cross
    .dependsOn(api)

lazy val pantsProbeApi213 = pantsProbeApi(scala213)

lazy val pantsProbePlugin =
  ideaPluginModule(id = "pants-probe-plugin", path = "extensions/pants/probePlugin", publish = true)
    .settings(
      intellijPluginName := "ideprobe-pants",
      packageArtifactZipFilter := { file: File =>
        // We want only this main jar to be packaged, all the library dependencies
        // are already in the probePlugin which will be available in runtime as we
        // depend on it in plugin.xml.
        // The packaging plugin is created to support one plugin per build, so there
        // seems to be no way to prevent including probePlugin.jar in the dist reasonable way.
        file.getName.contains("pants-probe-plugin")
      },
      name := "pants-probe-plugin",
      intellijPlugins ++= Seq(
        "PythonCore".toPlugin,
        "org.intellij.scala".toPlugin,
        "com.intellij.plugins.pants:1.15.1.42d84c497b639ef81ebdae8328401e3966588b2c:bleedingedge".toPlugin
      )
    )
    .cross
    .dependsOn(probePlugin, pantsProbeApi)

lazy val pantsProbePlugin213 = pantsProbePlugin(scala213)

lazy val pantsProbeDriver =
  module(id = "pants-probe-driver", path = "extensions/pants/driver")
    .settings(name := "pants-probe-driver")
    .cross
    .dependsOn(pantsProbeApi, driver, robotDriver)

lazy val pantsProbeDriver213 = pantsProbeDriver(scala213)
  .usesIdeaPlugins(pantsProbePlugin213, pantsProbePlugin212)

// bazel extension
lazy val bazelProbeApi =
  project(id = "bazel-probe-api", path = "extensions/bazel/api", publish = true).cross
    .dependsOn(api)

lazy val bazelProbeApi213 = bazelProbeApi(scala213)

lazy val bazelProbePlugin =
  ideaPluginModule(id = "bazel-probe-plugin", path = "extensions/bazel/probePlugin", publish = true)
    .settings(
      intellijPluginName := "ideprobe-bazel",
      packageArtifactZipFilter := { file: File =>
        // We want only this main jar to be packaged, all the library dependencies
        // are already in the probePlugin which will be available in runtime as we
        // depend on it in plugin.xml.
        // The packaging plugin is created to support one plugin per build, so there
        // seems to be no way to prevent including probePlugin.jar in the dist reasonable way.
        file.getName.contains("bazel-probe-plugin")
      },
      name := "bazel-probe-plugin",
      intellijPlugins ++= Seq(
        "com.google.idea.bazel.ijwb:2020.12.01.0.1".toPlugin
      )
    )
    .cross
    .dependsOn(probePlugin, bazelProbeApi)

lazy val bazelProbePlugin213 = bazelProbePlugin(scala213)

lazy val bazelProbeDriver =
  module(id = "bazel-probe-driver", path = "extensions/bazel/driver")
    .settings(
      name := "bazel-probe-driver",
      libraryDependencies += "commons-codec" % "commons-codec" % "1.15"
    )
    .cross
    .dependsOn(bazelProbeApi, driver, robotDriver)

lazy val bazelProbeDriver213 = bazelProbeDriver(scala213)
  .usesIdeaPlugins(bazelProbePlugin213, bazelProbePlugin212)

// examples
lazy val examples = testModule("examples", "examples")
  .settings(libraryDependencies ++= Dependencies.junit)
  .cross
  .dependsOn(driver, robotDriver, scalaProbeDriver)

lazy val examples213 = examples(scala213)

lazy val benchmarks = module("benchmarks", "benchmarks")
  .cross
  .dependsOn(driver)

lazy val benchmarks213 = benchmarks(scala213)

val commonSettings = Seq(
  libraryDependencies ++= Dependencies.junit
)

// 2.12
lazy val api212 = api(scala212)
lazy val driver212 = driver(scala212).usesIdeaPlugins(probePlugin212, probePlugin213)
lazy val robotDriver212 = robotDriver(scala212)
lazy val driverTests212 = driverTests(scala212).usesIdeaPlugins(driverTestPlugin212, driverTestPlugin213)
lazy val probePlugin212 = probePlugin(scala212).settings(libraryDependencies ++= Dependencies.scalaLib(scala212))
lazy val driverTestPlugin212 = driverTestPlugin(scala212)
lazy val junitDriver212 = junitDriver(scala212)
lazy val scalaProbeApi212 = scalaProbeApi(scala212)
lazy val scalaProbePlugin212 =
  scalaProbePlugin(scala212).settings(intellijPlugins += "org.intellij.scala:2022.1.14".toPlugin)
lazy val scalaProbeDriver212 = scalaProbeDriver(scala212).usesIdeaPlugins(scalaProbePlugin212, scalaProbePlugin213)
lazy val pantsProbeApi212 = pantsProbeApi(scala212)
lazy val pantsProbePlugin212 = pantsProbePlugin(scala212)
lazy val pantsProbeDriver212 = pantsProbeDriver(scala212).usesIdeaPlugins(pantsProbePlugin212, pantsProbePlugin213)
lazy val bazelProbeApi212 = bazelProbeApi(scala212)
lazy val bazelProbePlugin212 = bazelProbePlugin(scala212)
lazy val bazelProbeDriver212 = bazelProbeDriver(scala212).usesIdeaPlugins(bazelProbePlugin212, bazelProbePlugin213)
lazy val scalaTests212 = scalaTests(scala212).usesIdeaPlugin(scalaProbePlugin212)
lazy val benchmarks212 = benchmarks(scala212)

def project(id: String, path: String, publish: Boolean): Project = {
  Project(id, sbt.file(path))
    .settings(
      (Keys.publish / skip) := !publish,
      libraryDependencies ++= Dependencies.junit
    )
}

def module(id: String, path: String): Project = {
  project(id, path, publish = true).disableIdeaPluginDevelopment
    .settings(
      (Keys.publish / skip) := false
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
