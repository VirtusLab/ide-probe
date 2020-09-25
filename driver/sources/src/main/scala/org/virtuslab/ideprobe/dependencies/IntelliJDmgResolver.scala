package org.virtuslab.ideprobe.dependencies

object IntelliJDmgResolver {
  private val baseUri = "https://download.jetbrains.com"

  val community: DependencyResolver[IntelliJVersion] = resolver("ideaIC")

  val ultimate: DependencyResolver[IntelliJVersion] = resolver("ideaIU")

  def resolver(
      artifactName: String,
      group: String = "idea"
  ): DependencyResolver[IntelliJVersion] = { version: IntelliJVersion =>
    Dependency(s"$baseUri/$group/$artifactName-${version.release}.dmg")
  }
}
