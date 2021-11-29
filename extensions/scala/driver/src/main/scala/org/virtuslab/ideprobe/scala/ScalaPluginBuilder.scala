package org.virtuslab.ideprobe
package scala

import java.io.{File, InputStream}
import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies._
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

object ScalaPluginBuilder extends DependencyBuilder(Id("scala")) {

  case class Params(repository: GitRepository, jdk: Option[String])

  implicit val paramsReader: ConfigReader[Params] = deriveReader[Params]

  override def build(config: Config, resources: ResourceProvider): Path = {
    build(config.as[Params], resources)
  }

  def build(params: Params, resources: ResourceProvider): Path = {
    val repository = params.repository
    val jdkVersion = params.jdk.getOrElse("11")
    val hash = GitRepository.commitHash(repository, "HEAD")
    val artifact = repository.path.resolveChild(hash)
    resources.get(artifact, provider = build(repository, jdkVersion, resources))
  }

  private def build(
      repository: GitRepository,
      jdkVersion: String,
      resources: ResourceProvider
  ): InputStream = {
    val localRepo = GitRepository.clone(repository)
    runSbtPackageArtifact(resources, localRepo, Jdks.find(jdkVersion))
    localRepo.resolve("target/Scala-0.1.0-SNAPSHOT.zip").inputStream
  }

  private def runSbtPackageArtifact(
      resources: ResourceProvider,
      localRepo: Path,
      jdk: JdkInstaller
  ): Unit = {
    val jdkPath = jdk.install(resources)
    val env = Map("PATH" -> (jdkPath + File.pathSeparator + sys.env("PATH")))
    val command = List("sbt", "packageArtifactZip")
    val result = Shell.run(localRepo, env, command: _*)
    if (result.isFailed) error(s"Couldn't build scala plugin")
    println("Built scala plugin")
  }

}
