package org.virtuslab.ideprobe.dependencies

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.ide.intellij.IntelliJProvider

import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.{Config, IntelliJFixture}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnit4])
final class IntelliJProviderTest {
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  @Test
  def intelliJProviderShouldBeAbleToCorrectlyReadTheExistingInstanceVersion: Unit = givenInstalledIntelliJ {
    installationRoot =>
      //when trying to read the installed instance's version
      val intelliJVersion = IntelliJVersionResolver.version(installationRoot)

      //then
      assert(
        intelliJVersion.build == IntelliJProvider.Default.version.build,
        s"Expected ${IntelliJProvider.Default.version}, but got $intelliJVersion."
      )
  }

  @Test
  def existingIntelliJShouldNotBeDeletedDuringCleanup: Unit = givenInstalledIntelliJ { installationRoot =>
    val config = Config.fromString(s"""
      |probe.intellij {
      |    path = $installationRoot
      |    plugins = []
      |}
      |""".stripMargin)

    val fixture = IntelliJFixture.fromConfig(config)

    val existingInstalledIntelliJ = fixture.installIntelliJ()

    //when
    existingInstalledIntelliJ.cleanup()

    //then
    assert(
      installationRoot.isDirectory,
      "The provided IntelliJ instance should exist after cleanup, but it was deleted."
    )
  }


  @Test
  def shouldInstallIntellijFromExtractedRepository: Unit = givenInstalledIntelliJ { installationRoot =>

    val build = IntelliJVersion.Latest.build
    val installationPattern = installationRoot.toString.replace(build, "[revision]")
    val config = Config.fromString(s"""
                                      |probe.intellij {
                                      |    repositories = [\"$installationPattern\"]
                                      |}
                                      |""".stripMargin)

    val fixture = IntelliJFixture.fromConfig(config)

    val existingInstalledIntelliJ = fixture.installIntelliJ()

    //when
    existingInstalledIntelliJ.cleanup()

    //then
    assert(!existingInstalledIntelliJ.paths.root.isDirectory, "The provided IntelliJ instance should not exist after cleanup, but it was deleted.")
  }

  @Test
  def existingIntelliJShouldRetainItsOriginalPluginsDuringCleanup: Unit = givenInstalledIntelliJ { installationRoot =>
    //given a pre-installed IntelliJ and an IntelliJProvider
    val preInstalledPlugins = installationRoot.resolve("plugins").directChildren().toSet

    val config = Config.fromString(s"""
      |probe.intellij {
      |    path = $installationRoot
      |    plugins = [
      |      { id = "org.intellij.scala", version = "2020.1.27" }
      |    ]
      |  }
      |""".stripMargin)

    val fixture = IntelliJFixture.fromConfig(config)

    val existingIntelliJ = fixture.installIntelliJ()
    val installedPlugins = existingIntelliJ.paths.bundledPlugins.directChildren().toSet

    assert(installedPlugins.diff(preInstalledPlugins).nonEmpty, "No plugins were installed.")

    //when cleanup is called
    existingIntelliJ.cleanup()

    //then plugins after cleanup should be the same as initially
    val pluginsAfterCleanup = existingIntelliJ.paths.bundledPlugins.directChildren().toSet
    assert(installedPlugins.diff(pluginsAfterCleanup).nonEmpty, "No plugins were removed during cleanup.")
    assert(
      pluginsAfterCleanup == preInstalledPlugins,
      "Plugins after cleanup should be the same as before, but found following plugins still present after cleanup:" +
        s" ${pluginsAfterCleanup.diff(preInstalledPlugins).mkString(", ")}," +
        s" and following plugins missing: ${preInstalledPlugins.diff(pluginsAfterCleanup).mkString(", ")}."
    )
  }

  private def givenInstalledIntelliJ(test: Path => Unit): Unit = {
    val preInstalledIntelliJ = IntelliJProvider.Default.setup()
    val installationRoot = preInstalledIntelliJ.paths.root
    preInstalledIntelliJ.paths.bundledPlugins
      .resolve("ideprobe")
      .delete() //Removing the ideprobe plugin - to avoid conflicts when installing it in tests.
    try test(installationRoot)
    finally preInstalledIntelliJ.cleanup()
  }
}
