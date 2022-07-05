package org.virtuslab.ideprobe.dependencies

import java.nio.file.Files
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import org.junit.Assert._
import org.junit.Test

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IntelliJFixture

final class FixtureConfigTest extends WorkspaceFixture {
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  /**
   * Note that the validity of the properties cannot be check during creation, nor it should be validated then. This
   * test check only that a rule can be created with a given set of properties.
   */
  @Test
  def createsFixtureFromConfFile(): Unit = {
    try {
      IntelliJFixture.fromConfig(Config.fromClasspath("example.conf"))
    } catch {
      case e: Throwable => fail(s"Could not create test fixture due to: ${e.getMessage}")
    }
  }

  @Test
  def usesProvidedWorkspaceDirectory(): Unit = withWorkspace { workspace =>
    val file = Files.createTempFile(workspace, "ideprobe", "file")
    val config = Config.fromString(s"""probe.workspace.path = "$workspace" """)

    IntelliJFixture.fromConfig(config).withWorkspace { workspace =>
      val workspaceFile = workspace.path.resolve(file.getFileName)
      assertTrue(s"Invalid workspace used: ${workspace.path}", Files.exists(workspaceFile))
    }
  }

  @Test
  def exposesEnvironmentFromConfigFile(): Unit = withWorkspace { workspace =>
    val config = Config.fromString(s"""
         |probe.workspace.path = "$workspace"
         |key = "value"
         |""".stripMargin)

    val rule = IntelliJFixture.fromConfig(config)

    rule.config.get[String]("key") match {
      case Some(value) =>
        assertEquals("value", value)
      case None =>
        fail("key not found in config")
    }
  }

  @Test
  def clonesGitRepositoryToWorkspace(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"

    val config = Config.fromString(s"""probe.workspace.path = "$publicRepository" """)

    IntelliJFixture.fromConfig(config).withWorkspace { workspace =>
      assertTrue(workspace.path.resolve(".git").isDirectory)
    }
  }

  @Test
  def clonesSpecificGitBranchToWorkspace(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"
    val branch = "develop"

    val config = Config.fromString(s"""
          |probe.workspace {
          |  path = "$publicRepository"
          |  branch = "$branch"
          |}
          |""".stripMargin)

    IntelliJFixture.fromConfig(config).withWorkspace { workspace =>
      val HEAD = workspace.path.resolve(".git/HEAD").content().trim
      assertEquals(HEAD, s"ref: refs/heads/$branch")
    }
  }

  @Test
  def checksOutRefByHash(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"
    val commit = "a1861fc3b70588acfa171000eb365bf75c143472"

    val config = Config.fromString(s"""
          |probe.workspace {
          |  path = "$publicRepository"
          |  commit = "$commit"
          |}
          |""".stripMargin)

    IntelliJFixture.fromConfig(config).withWorkspace { workspace =>
      val HEAD = workspace.path.resolve(".git/HEAD").content().trim
      assertEquals(HEAD, commit)
    }
  }

  @Test
  def ignoresEmptyPlugin(): Unit = {
    val config = Config.fromString("""probe.intellij.plugins = [ { uri = ${?NO_SUCH_ENV} } ]""")
    val plugins = IntelliJFixture.fromConfig(config).intelliJProvider.plugins
    assertEquals(Nil, plugins)
  }
}
