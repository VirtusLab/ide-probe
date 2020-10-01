package org.virtuslab.ideprobe.dependencies

import java.net.URI
import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.{OS, Shell, error}

class JdkInstaller(name: String, uris: Map[OS, URI]) {
  def install(resources: ResourceProvider): Path = {
    val installationDir = Paths.get(sys.props("java.io.tmpdir"), "ideprobe", "jdk").resolve(name)

    if (installationDir.isDirectory) installationDir
    else {
      val uri = uris.getOrElse(OS.Current, error(s"$name is not available for this OS"))
      val path = resources.get(uri)
      val result = Shell.run(
        "tar",
        "xzf",
        path.toString,
        "-C",
        installationDir.createDirectory().toString,
        "--strip-components=1"
      )
      if (!result.isSuccess) {
        installationDir.delete()
        error(s"Cannot unpack $this from $path.")
      }
      installationDir
    }
  }
}

object Jdks {
  val JDK_8: JdkInstaller = new JdkInstaller(
    "jdk-8",
    Map(
      OS.Unix -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09/OpenJDK8U-jdk_x64_linux_hotspot_8u252b09.tar.gz"
      ),
      OS.Mac -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u265-b01/OpenJDK8U-jdk_x64_mac_hotspot_8u265b01.tar.gz"
      )
    )
  )
  val JDK_11: JdkInstaller = new JdkInstaller(
    "jdk-11",
    Map(
      OS.Mac -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10_openj9-0.21.0/OpenJDK11U-jdk_x64_mac_openj9_11.0.8_10_openj9-0.21.0.tar.gz"
      ),
      OS.Unix -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10_openj9-0.21.0/OpenJDK11U-jdk_x64_linux_openj9_11.0.8_10_openj9-0.21.0.tar.gz"
      )
    )
  )
}
