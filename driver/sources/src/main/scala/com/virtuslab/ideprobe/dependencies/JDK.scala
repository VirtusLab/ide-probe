package com.virtuslab.ideprobe.dependencies

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import com.virtuslab.ideprobe.Extensions._
import com.virtuslab.ideprobe.Shell
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

object JDK {
  sealed case class Version(name: String)
  val JDK_1_8: Version = Version("jdk-8")

  def apply(version: Version, resources: ResourceProvider): Path = Provider(version, resources)

  private val sources = Map[Version, URI](
    JDK_1_8 -> URI.create(
      "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09/OpenJDK8U-jdk_x64_linux_hotspot_8u252b09.tar.gz"
    )
  )

  object Provider extends LibraryProvider[Version] {
    override def apply(key: Version, resources: ResourceProvider): Path = {
      val uri = sources(key)
      val installationDir = LibraryProvider.DefaultDir.resolve(key.name)

      if (installationDir.isDirectory) installationDir
      else
        resources.get(uri) match {
          case Resource.File(path) =>
            val result = Shell.run(
              "tar",
              "xzf",
              path.toString,
              "-C",
              installationDir.createDirectory().toString,
              "--strip-components=1"
            )
            if (result.exitCode != 0) {
              installationDir.delete()
              throw new Exception(s"Cannot unpack $key from $path. STDERR: ${result.err}")
            }
            installationDir
          case resource =>
            throw new Exception(s"Cannot unpack $key from $resource")
        }
    }
  }

}
