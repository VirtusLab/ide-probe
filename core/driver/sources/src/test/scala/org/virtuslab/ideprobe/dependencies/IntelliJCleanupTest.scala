package org.virtuslab.ideprobe.dependencies

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.config.{DependenciesConfig, IdeProbeConfig, IntellijConfig, PathsConfig}
import org.virtuslab.ideprobe.ide.intellij.{IntelliJFactory, IntelliJProvider}

import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions._

@RunWith(classOf[JUnit4])
final class IntelliJCleanupTest {
  @Test
  def existingIntelliJShouldNotBeDeletedDuringCleanup: Unit = givenInstalledIntelliJ { installationRoot =>
      val intelliJProvider = IntelliJProvider.from(
        IdeProbeConfig(
          intellij = IntellijConfig.Existing(installationRoot, Seq.empty),
          workspace = None,
          resolvers = DependenciesConfig.Resolvers(),
          driver = IntelliJFactory.Default.config,
          paths = PathsConfig()
        )
      )
      val existingInstalledIntelliJ = intelliJProvider.setup()

      //when
      existingInstalledIntelliJ.cleanup()

      //then
      assert(installationRoot.isDirectory, "The provided IntelliJ instance should exist after cleanup, but it was deleted.")
    }

  @Test
  def existingIntelliJShouldRetainItsOriginalPluginsDuringCleanup: Unit = givenInstalledIntelliJ { installationRoot =>
    //given a pre-installed IntelliJ and an IntelliJProvider
    val preInstalledPlugins = installationRoot.resolve("plugins").toFile.list().toSet
    val pluginsToInstall = Seq( //Plugins choice is arbitrary.
      Plugin.Versioned("9667", "1.0.298", None),
      Plugin.Versioned("8327", "2021.1-1.5.9", None)
    )
    val intelliJProvider = IntelliJProvider.from(
      IdeProbeConfig(
        intellij = IntellijConfig.Existing(installationRoot, pluginsToInstall),
        workspace = None,
        resolvers = DependenciesConfig.Resolvers(),
        driver = IntelliJFactory.Default.config,
        paths = PathsConfig()
      )
    )

    val existingIntelliJ = intelliJProvider.setup()
    val installedPlugins = existingIntelliJ.paths.plugins.toFile.list().toSet

    assert((installedPlugins diff preInstalledPlugins).nonEmpty, "No plugins were installed.")

    //when cleanup is called
    existingIntelliJ.cleanup()

    //then plugins after cleanup should be the same as initially
    val pluginsAfterCleanup = existingIntelliJ.paths.plugins.toFile.list().toSet
    assert((installedPlugins diff pluginsAfterCleanup).nonEmpty, "No plugins were removed during cleanup.")
    assert(
      pluginsAfterCleanup == preInstalledPlugins,
      "Plugins after cleanup should be the same as before, but found following plugins still present after cleanup:" +
        s" ${(pluginsAfterCleanup diff preInstalledPlugins).mkString(", ")}," +
        s" and following plugins missing: ${(preInstalledPlugins diff pluginsAfterCleanup).mkString(", ")}."
    )
  }

  private def givenInstalledIntelliJ(test: Path => Unit): Unit = {
    val preInstalledIntelliJ = IntelliJFactory.Default.create(IntelliJVersion.Latest, Seq.empty)
    val installationRoot = preInstalledIntelliJ.paths.plugins.getParent //plugins directory is an arbitrary choice.
    preInstalledIntelliJ.paths.plugins.resolve("ideprobe").delete() //Removing the ideprobe plugin - to avoid conflicts when installing it in tests.
    try test(installationRoot)
    finally preInstalledIntelliJ.cleanup()
  }
}
