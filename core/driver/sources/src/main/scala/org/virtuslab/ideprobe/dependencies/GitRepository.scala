package org.virtuslab.ideprobe
package dependencies

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

final case class GitRepository(path: URI, ref: Option[String])

object GitRepository extends ConfigFormat {
  implicit val convert: ConfigConvert[GitRepository] = deriveConvert[GitRepository]

  def clone(repository: GitRepository, name: String = "git-repository"): Path = {
    val localRepo = Files.createTempDirectory(name)
    val cloned = Shell.run("git", "clone", repository.path.toString, localRepo.toString)
    if (cloned.exitCode != 0) throw new IllegalStateException(s"Could not clone git $repository")
    repository.ref.foreach { ref =>
      val checkout = Shell.run(in = localRepo, "git", "checkout", ref)
      if (checkout.exitCode != 0) throw new IllegalStateException(s"Could not checkout $ref in $repository")
    }
    println(s"Cloned $repository")
    localRepo
  }

  def commitHash(repository: GitRepository, fallbackRef: String): String = {
    val ref = repository.ref.getOrElse(fallbackRef)
    val result = Shell.run("git", "ls-remote", repository.path.toString, ref)

    if (result.isFailed) {
      error(s"Could not fetch hashes from ${repository.path}")
    }
    val hash = result.out.linesIterator.map(_.split("\\W+")).collectFirst { case Array(hash, `ref`) =>
      hash
    }
    hash.orElse(repository.ref).getOrElse(error(s"Ref $ref not found"))
  }

}
