package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.config.DependenciesConfig
import org.virtuslab.ideprobe.config.DependenciesConfig.IntelliJ
import org.virtuslab.ideprobe.dependencies.Dependency.Artifact

object NightlyIntelliJZipResolver
    extends IntelliJPatternResolver("https://download.jetbrains.com/idea/nightly/[artifact]-[revision].portable.zip")

object AlternativeIntelliJZipResolver
    extends IntelliJPatternResolver("https://download.jetbrains.com/idea/[artifact]-[revision].portable.zip")

object IntelliJZipResolver extends IntelliJResolver {

  val community: DependencyResolver[IntelliJVersion] = official("ideaIC")

  val ultimate: DependencyResolver[IntelliJVersion] = official("ideaIU")

  private def official(artifact: String): DependencyResolver[IntelliJVersion] = {
    val officialUri = "https://www.jetbrains.com/intellij-repository"
    val officialReleases = fromMaven(s"$officialUri/releases", artifact)
    val officialSnapshots = fromMaven(s"$officialUri/snapshots", artifact)
    (version: IntelliJVersion) => {
      if (version.build.endsWith("SNAPSHOT")) officialSnapshots.resolve(version)
      else {
        val dependency = officialReleases.resolve(version)
        dependency match {
          case Artifact(uri) if Resource.exists(uri) => dependency
          case Artifact(_) => officialSnapshots.resolve(version.copy(build = version.build + "-EAP-SNAPSHOT"))
          case _           => dependency
        }
      }
    }
  }

  def fromMaven(uri: String, artifact: String): DependencyResolver[IntelliJVersion] = {
    IntelliJPatternResolver(s"$uri/[orgPath]/[module]/[artifact]/[revision]/[artifact]-[revision].zip")
      .resolver(artifact)
  }

  // this method enables downloading .zip IntelliJ even if `probe.intellij.version.ext` value is not ".zip"
  def fromConfig(config: DependenciesConfig.Resolvers): Seq[DependencyResolver[IntelliJVersion]] = {
    val configWithZipRepositories =
      config.copy(intellij = IntelliJ(config.intellij.repositories.map(_.replace("[ext]", ".zip"))))
    IntelliJResolver.fromConfig(configWithZipRepositories)
  }
}
