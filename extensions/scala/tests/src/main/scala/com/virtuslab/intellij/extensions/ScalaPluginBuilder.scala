package com.virtuslab.intellij.extensions

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import com.virtuslab.ideprobe.Extensions._
import com.virtuslab.ideprobe.Id
import com.virtuslab.ideprobe.Shell
import com.virtuslab.ideprobe.dependencies.DependencyBuilder
import com.virtuslab.ideprobe.dependencies.JDK
import com.virtuslab.ideprobe.dependencies.JDK.JDK_1_8
import com.virtuslab.ideprobe.dependencies.Resource
import com.virtuslab.ideprobe.dependencies.ResourceProvider
import com.virtuslab.ideprobe.dependencies.SourceRepository
import com.virtuslab.ideprobe.dependencies.SourceRepository.Git

object ScalaPluginBuilder extends DependencyBuilder(Id("scala")) {
  def build(repository: SourceRepository, resources: ResourceProvider): Resource =
    repository match {
      case git: Git =>
        val hash = this.hash(git, "HEAD")
        val artifact = git.path.resolveChild(hash)

        resources.get(artifact, provider = () => build(git, resources))
    }

  private def build(repository: Git, resources: ResourceProvider): InputStream = {
    val localRepo = clone(repository)

    val jdk = JDK(JDK_1_8, resources).resolve("bin")
    val env = Map("PATH" -> (jdk + File.pathSeparator + sys.env("PATH")))
    val command = List("sbt", "packageArtifactZip")
    val result = Shell.run(localRepo, env, command: _*)
    if (result.exitCode != 0) throw new Exception(s"Couldn't build scala plugin. STDERR:\n${result.err}")
    println("Built scala plugin")

    localRepo.resolve("target/Scala-0.1-SNAPSHOT.zip").inputStream
  }

  private def clone(repository: Git) = {
    val localRepo = Files.createTempDirectory("scala-plugin-repo")
    val command: Array[String] = repository.branch match {
      case None         => Array("git", "clone", repository.path.toString, localRepo.toString)
      case Some(branch) => Array("git", "clone", repository.path.toString, branch, localRepo.toString)
    }
    val result = Shell.run(command: _*)
    if (result.exitCode != 0) throw new Exception(s"Could not clone $repository. STDERR:\n${result.err}")

    println(s"Cloned $repository")
    localRepo
  }

  private def hash(repository: Git, fallbackRef: String) = {
    val Ref = repository.branch.getOrElse(fallbackRef)
    val result = Shell.run("git", "ls-remote", repository.path.toString, Ref)

    if (result.exitCode != 0)
      throw new Exception(s"Could not fetch hashes from ${repository.path}. STDERR:\n${result.err}")
    val hash = result.out.linesIterator.map(_.split("\\W+")).collectFirst {
      case Array(hash, Ref) => hash
    }
    hash.getOrElse(throw new Exception(s"Ref $Ref not found"))
  }
}
