package org.virtuslab.ideprobe.pants

import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.Id
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.dependencies.DependencyBuilder
import org.virtuslab.ideprobe.dependencies.GitRepository
import org.virtuslab.ideprobe.dependencies.ResourceProvider
import org.virtuslab.ideprobe.error

object PantsPluginBuilder extends DependencyBuilder(Id("pants")) {
  def build(config: Config, resources: ResourceProvider): Path = {
    val repository = config[GitRepository]("repository")
    val env = config.get[Map[String, String]]("env").getOrElse(Map.empty)
    val hash = GitRepository.commitHash(repository, "HEAD")
    val artifact = repository.path.resolveChild(hash)
    resources.get(artifact, provider = () => build(repository, env))
  }

  private def build(repository: GitRepository, userEnv: Map[String, String]): InputStream = {
    val localRepo = GitRepository.clone(repository)
    val env = Map("PANTS_SHA" -> "33735fe23228472367dc73f26bb96a755452192f") ++ userEnv
    Shell.run(localRepo, env, "./scripts/setup-ci-environment.sh").assertSuccess()
    Shell.run(localRepo, env, "./gradlew", ":buildPlugin").assertSuccess()

    val files = localRepo.resolve("build/distributions").directChildren()
    val output = files.find(_.name.matches("pants.*\\.zip")).getOrElse {
      error(s"Couldn't find pants archive. Existing files:\n${files.mkString("\n")}")
    }

    val pantsPath = Paths.get(sys.props("java.io.tmpdir"), s"pants${UUID.randomUUID()}.zip")
    output.moveTo(pantsPath)
    if (pantsPath.isFile) {
      println(s"Built pants at $pantsPath")
      localRepo.delete()
      pantsPath.inputStream
    } else {
      error(s"Could not move $output to $pantsPath")
    }
  }

}
