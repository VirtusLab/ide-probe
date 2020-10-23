package org.virtuslab.ideprobe.dependencies

import java.net.URI
import org.virtuslab.ideprobe.Extensions._

class IntelliJDmgResolver(baseUri: URI) {
  val community: DependencyResolver[IntelliJVersion] = resolver("ideaIC")

  val ultimate: DependencyResolver[IntelliJVersion] = resolver("ideaIU")

  def resolver(artifactName: String): DependencyResolver[IntelliJVersion] = { version: IntelliJVersion =>
    val name = s"$artifactName-${version.release.getOrElse(version.build)}.dmg"
    Dependency.Artifact(baseUri.resolveChild(name))
  }
}

object OfficialIntelliJDmgResolver extends IntelliJDmgResolver(URI.create("https://download.jetbrains.com/idea"))
