package org.virtuslab.ideprobe.bazel

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import pureconfig.generic.auto._

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.OS
import org.virtuslab.ideprobe.error

trait BazeliskExtension extends ConfigFormat {

  private case class DownloadArtifact(url: URL, sha256sum: String)

  protected def installBazelisk(bazelExecPath: Path, config: Config): Unit = {
    val osKey = OS.Current match {
      case OS.Unix => "linux"
      case OS.Mac  => "mac"
      case other   => error(s"Unsupported os: $other")
    }
    config.get[DownloadArtifact](s"bazelisk.$osKey").foreach { artifact =>
      downloadFile(artifact, bazelExecPath)
      bazelExecPath.makeExecutable()
    }
  }

  protected def bazelPath(workspace: Path): Path = {
    workspace.resolve("bin/bazel")
  }

  private def downloadFile(artifact: DownloadArtifact, path: Path): Unit = {
    println(s"Downloading ${artifact.url} to $path")
    FileUtils.copyURLToFile(artifact.url, path.toFile)
    val checksum = DigestUtils.sha256Hex(Files.newInputStream(path))
    if (checksum != artifact.sha256sum) {
      error(s"Invalid checksum for '${artifact.url}', expected: ${artifact.sha256sum}, actual: $checksum")
    }
  }
}
