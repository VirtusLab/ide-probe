package org.virtuslab.ideprobe
package dependencies

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.dependencies.git.GitHandler

final case class GitRepository(path: URI, ref: Option[String])

object GitRepository extends ConfigFormat {
  implicit val convert: ConfigConvert[GitRepository] = deriveConvert[GitRepository]

  def clone(repository: GitRepository, name: String = "git-repository"): Path = {
    val localRepo = Files.createTempDirectory(name)
    val git = GitHandler.clone(repository.path, localRepo)
    repository.ref.foreach { ref =>
      git.checkout(ref)
    }
    println(s"Cloned $repository")
    localRepo
  }

  def commitHash(repository: GitRepository, fallbackRef: String): String = {
    val ref = repository.ref.getOrElse(fallbackRef)
    val hash = GitHandler.commitHash(repository.path, ref)
    hash.orElse(repository.ref).getOrElse(error(s"Ref $ref not found"))
  }

}
