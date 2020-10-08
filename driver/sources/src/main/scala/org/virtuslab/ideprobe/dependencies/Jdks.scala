package org.virtuslab.ideprobe.dependencies

import java.net.URI
import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.{OS, Shell, error}

class JdkInstaller(val name: String, uris: Map[OS, URI]) {
  def install(resources: ResourceProvider): Path = {
    val installationDir = Paths.get(sys.props("java.io.tmpdir"), "ideprobe", "jdk").resolve(name)
    val javaBinaryPath = findJavaBinary(installationDir)

    if (javaBinaryPath.isFile) {
      javaBinaryPath.getParent
    } else {
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
      } else {
        javaBinaryPath.getParent
      }
    }
  }

  private def findJavaBinary(root: Path): Path = {
    OS.Current match {
      case OS.Mac     => root.resolve("Contents/Home/bin/java")
      case OS.Unix    => root.resolve("bin/java")
      case OS.Windows => root.resolve("bin/java.exe")
    }
  }
}

object Jdks {
  private var installers = Seq.empty[JdkInstaller]

  private def registerJdk(name: String, uris: Map[OS, URI]): JdkInstaller = {
    val installer = new JdkInstaller(name, uris)
    installers :+= installer
    installer
  }

  def find(version: String): JdkInstaller = {
    installers
      .find(_.name == version)
      .getOrElse(error(s"Jdk not found, available versions are ${installers.mkString("[", ", ", "]")}"))
  }

  val JDK_8: JdkInstaller = registerJdk(
    "8",
    Map(
      OS.Unix -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09/OpenJDK8U-jdk_x64_linux_hotspot_8u252b09.tar.gz"
      ),
      OS.Mac -> URI.create(
        "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u265-b01/OpenJDK8U-jdk_x64_mac_hotspot_8u265b01.tar.gz"
      )
    )
  )
  val JDK_11: JdkInstaller = registerJdk(
    "11",
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
