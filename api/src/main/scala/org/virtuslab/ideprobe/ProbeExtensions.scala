package org.virtuslab.ideprobe

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.FileVisitResult
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream
import scala.util.control.NonFatal

trait ProbeExtensions {

  implicit final class URIExtension(uri: URI) {
    def resolveChild(name: String): URI = {
      if (uri.getPath.endsWith("/")) uri.resolve(name)
      else URI.create(s"$uri/$name")
    }
  }

  implicit final class PathExtension(path: Path) {
    def name: String = {
      path.getFileName.toString
    }

    def isFile: Boolean = {
      Files.isRegularFile(path)
    }

    def isDirectory: Boolean = {
      Files.isDirectory(path)
    }

    def createParentDirectory(): Path = {
      Files.createDirectories(path.getParent)
    }

    def createDirectory(): Path = {
      Files.createDirectories(path)
    }

    def createDirectory(name: String): Path = {
      Files.createDirectories(path.resolve(name))
    }

    def copyTo(target: Path): Path = {
      Files.copy(path, target)
    }

    def moveTo(target: Path): Path = {
      target.createParentDirectory()
      Files.move(path, target)
    }

    def write(content: String): Path = {
      path.createParentDirectory()
      Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    def append(content: InputStream): Path = {
      val out = path.outputStream
      try {
        content.writeTo(out)
        path
      } finally {
        Close(content, out)
      }
    }

    def createFile(content: InputStream): Path = {
      path.createEmptyFile().append(content)
    }

    def createEmptyFile(): Path = {
      path.getParent.createDirectory()
      Files.createFile(path)
    }

    def outputStream: OutputStream = {
      val output = Files.newOutputStream(path)
      new BufferedOutputStream(output)
    }

    def inputStream: InputStream = {
      val input = Files.newInputStream(path)
      new BufferedInputStream(input)
    }

    def makeExecutable(): Path = {
      import java.nio.file.attribute.PosixFilePermission._
      val attributes = Files.getPosixFilePermissions(path)
      attributes.add(OWNER_EXECUTE)
      Files.setPosixFilePermissions(path, attributes)
    }

    def delete(): Unit = {
      val deletingVisitor = new ProbeExtensions.DeletingVisitor(path)
      Files.walkFileTree(path, deletingVisitor)
    }

    def content(): String = {
      new String(Files.readAllBytes(path))
    }

    def copyDir(targetDir: Path): Unit = {
      copyFiles(Files.walk(path), targetDir)
    }

    def copyDirContent(targetDir: Path): Unit = {
      copyFiles(Files.walk(path).skip(1), targetDir)
    }

    private def copyFiles(files: java.util.stream.Stream[Path], targetDir: Path): Unit = {
      try {
        files.forEach { source =>
          val target = targetDir.resolve(path.relativize(source))
          if (Files.isDirectory(source)) {
            target.createDirectory()
          } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
          }
        }
      } finally {
        files.close()
      }
    }

  }

  implicit final class ZipInputStreamExtension(zip: ZipInputStream) {
    def unpackTo(path: Path): Unit = {
      Files.createDirectories(path)
      try {
        val input = new InputStream {
          override def read(): Int = zip.read()
          override def read(b: Array[Byte]): Int = zip.read(b)
          override def read(b: Array[Byte], off: Int, len: Int): Int = zip.read(b, off, len)
          override def close(): Unit = () // we must not close zip after writing to the file
        }

        Iterator
          .continually(zip.getNextEntry)
          .takeWhile(_ != null)
          .filterNot(_.isDirectory)
          .map(entry => path.resolve(entry.getName))
          .foreach(target => target.createFile(input))
      } finally {
        Close(zip)
      }
    }
  }

  implicit final class InputStreamExtension(input: InputStream) {
    def writeTo(output: OutputStream): Unit = {
      val buffer = new Array[Byte](8096)
      Iterator
        .continually(input.read(buffer))
        .takeWhile(read => read >= 0)
        .foreach(read => output.write(buffer, 0, read))
      output.flush()
    }
  }
}

object ProbeExtensions {
  private class DeletingVisitor(root: Path) extends SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if(!Files.isDirectory(file))  Files.delete(file)
      FileVisitResult.CONTINUE
    }

    override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
      exc match {
        case nsf: NoSuchFileException =>
          FileVisitResult.CONTINUE
        case NonFatal(e) =>
          val message = s"Failure while deleting $root at file $file"
          val exception = new IOException(message, exc)
          exception.printStackTrace()
          FileVisitResult.TERMINATE
      }
    }

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      Option(exc) match {
        case None =>
          Files.delete(dir)
          FileVisitResult.CONTINUE
        case Some(e) =>
          val message = s"Failure while deleting $root at dir  $dir"
          val exception = new IOException(message, e)
          exception.printStackTrace()
          FileVisitResult.TERMINATE
      }
    }
  }
}
