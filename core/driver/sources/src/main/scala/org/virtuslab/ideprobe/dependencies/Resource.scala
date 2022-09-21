package org.virtuslab.ideprobe.dependencies

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.ProviderNotFoundException
import java.util.zip.ZipInputStream

import scala.util.control.NonFatal

import pureconfig.ConfigReader

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.OS

sealed trait Resource

object Resource extends ConfigFormat {
  implicit val resourceConfigReader: ConfigReader[Resource] = ConfigReader[String].map(from)

  def exists(uri: URI): Boolean = from(uri) match {
    case File(path) => Files.exists(path)
    case Http(uri) =>
      val connection = uri.toURL.openConnection()
      connection.connect()
      val responseCode = connection.asInstanceOf[HttpURLConnection].getResponseCode
      responseCode == 200
    case Unresolved(_, _) => true // can't verify, assume it exists
    case Jar(uri) =>
      throw new UnsupportedOperationException(
        s"Resolved URI: $uri points to a JAR file, which should not happen for a release of IntelliJ"
      )
  }

  def from(value: String): Resource = {
    try {
      from(URI.create(value))
    } catch {
      case NonFatal(e) =>
        Unresolved(value, e)
    }
  }

  @scala.annotation.tailrec
  def from(uri: URI): Resource = uri.getScheme match {
    case null             => File(Paths.get(uri.getPath))
    case "file"           => File(Paths.get(uri))
    case "jar"            => Jar(uri)
    case "http" | "https" => Http(uri)
    case "classpath"      => from(getClass.getResource(uri.getPath).toURI)
    case scheme =>
      Unresolved(uri.toString, new IllegalArgumentException(s"Unsupported scheme: $scheme"))
  }

  case class Jar(uri: URI) extends Resource
  case class File(path: Path) extends Resource
  case class Http(uri: URI) extends Resource
  case class Unresolved(path: String, cause: Throwable) extends Resource

  sealed trait IntellijResource {
    def path: Path
    def installTo(target: Path): Unit

    def rootEntries: List[String] = {
      try {
        val fs = FileSystems.newFileSystem(path, this.getClass.getClassLoader)
        Files.list(fs.getPath("/")).iterator.asScala.map(_.toString).toList
      } catch {
        case _: ProviderNotFoundException =>
          throw new IllegalArgumentException(
            s"""
               |The path provided as the plugin path: $path is not valid.
               |Make sure it points to the plugin file (or .zip archive) and NOT to a directory.""".stripMargin
          )
      }
    }
  }

  final class Archive(val path: Path) extends IntellijResource {
    def installTo(target: Path): Unit = {
      val zip = new ZipInputStream(path.inputStream)
      zip.unpackTo(target)
    }
  }

  final class DMGFile(val path: Path) extends IntellijResource {
    def installTo(target: Path): Unit = {
      import sys.process._

      // create tmp directory where disk image will be attached
      val dmgDirPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("dmg_dir")
      dmgDirPath.createDirectory()
      val dmgDir = dmgDirPath.toString

      try {
        // attach disk image from .dmg file to local filesystem
        s"hdiutil attach -mountpoint $dmgDir ${path.toString}".!
        // copy $dmgDir/IntelliJ IDEA CE.app/ to proper installation directory
        val idePath = Paths.get(s"$dmgDir/IntelliJ IDEA CE.app/Contents")
        idePath.copyDir(target)
      } finally {
        // detach disk image from local filesystem
        s"hdiutil detach $dmgDir".!
      }

    }
  }

  final class Plain(val path: Path) extends IntellijResource {
    def installTo(target: Path): Unit = {
      path.copyDir(target)
    }
  }

  object Archive {
    private val ZipMagicNumber = 0x504b0304

    def unapply(path: Path): Option[Archive] = {
      import java.io.DataInputStream
      val stream = new DataInputStream(path.inputStream)
      try {
        val magicNumber = stream.readInt()
        if (magicNumber == ZipMagicNumber) Some(new Archive(path))
        else None
      } catch {
        case NonFatal(_) =>
          None
      } finally {
        stream.close()
      }
    }
  }

  object DMGFile {
    def unapply(path: Path): Option[DMGFile] = {
      if (path.toString.endsWith(".dmg") && OS.Current == OS.Mac) Some(new DMGFile(path)) else None
    }
  }

  object Plain {
    def unapply(path: Path): Option[Plain] = {
      if (path.isDirectory) Some(new Plain(path)) else None
    }
  }

  implicit class ExtractorExtension(path: Path) {
    def toExtracted: IntellijResource = path match {
      case Archive(archive) => archive
      case DMGFile(archive) => archive
      case Plain(archive)   => archive
      case _                => throw new IllegalStateException(s"Not an archive: $path")
    }
  }
}
