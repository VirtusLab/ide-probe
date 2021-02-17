package com.twitter.intellij.pants

import java.io.InputStream
import java.nio.file.{Path, Paths}
import java.util.UUID
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.{DependencyBuilder, GitRepository, ResourceProvider}
import org.virtuslab.ideprobe.{Config, Id, Shell, error}

object PantsPluginBuilder extends DependencyBuilder(Id("pants")) {
  def build(config: Config, resources: ResourceProvider): Path = {
    val repository = config[GitRepository]("repository")
    val env = config.get[Map[String, String]]("env").getOrElse(Map.empty)
    val hash = GitRepository.commitHash(repository, "HEAD")
    val artifact = repository.path.resolveChild(hash)
    resources.get(artifact, provider = build(repository, env))
  }

  private def build(repository: GitRepository, env: Map[String, String]): InputStream = {
    val localRepo = GitRepository.clone(repository)

    Shell.run(localRepo, env, "./scripts/setup-ci-environment.sh").ok()

    val deployEnv = env ++ Map("TRAVIS_BRANCH" -> "master")
    Shell.run(localRepo, deployEnv, "./scripts/deploy/deploy.sh", "--skip-publish").ok()

    val files = localRepo.directChildren()
    val output = files.find(_.name.matches("pants_.*\\.zip")).getOrElse {
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
