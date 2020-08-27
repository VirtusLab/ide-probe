package org.virtuslab.ideprobe.scala

import java.io.File
import java.io.InputStream
import java.nio.file.Files

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.Id
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.dependencies.DependencyBuilder
import org.virtuslab.ideprobe.dependencies.JDK
import org.virtuslab.ideprobe.dependencies.JDK.JDK_1_8
import org.virtuslab.ideprobe.dependencies.Resource
import org.virtuslab.ideprobe.dependencies.ResourceProvider
import org.virtuslab.ideprobe.dependencies.SourceRepository
import org.virtuslab.ideprobe.dependencies.SourceRepository.Git

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

    localRepo.resolve("target/Scala-0.1.0-SNAPSHOT.zip").inputStream
  }

  private def clone(repository: Git) = {
    val localRepo = Files.createTempDirectory("scala-plugin-repo")
    val cloned = Shell.run("git", "clone", repository.path.toString, localRepo.toString)
    if (cloned.exitCode != 0) throw new IllegalStateException(s"Could not clone git $repository")
    repository.ref.foreach { ref =>
      val checkout = Shell.run(in = localRepo, "git", "checkout", ref)
      if (checkout.exitCode != 0) throw new IllegalStateException(s"Could not checkout $ref in $repository")
    }
    println(s"Cloned $repository")
    localRepo
  }

  private def hash(repository: Git, fallbackRef: String) = {
    val Ref = repository.ref.getOrElse(fallbackRef)
    val result = Shell.run("git", "ls-remote", repository.path.toString, Ref)

    if (result.exitCode != 0)
      throw new Exception(s"Could not fetch hashes from ${repository.path}. STDERR:\n${result.err}")
    val hash = result.out.linesIterator.map(_.split("\\W+")).collectFirst {
      case Array(hash, Ref) => hash
    }
    hash.orElse(repository.ref).getOrElse(throw new Exception(s"Ref $Ref not found"))
  }
}
