package org.virtuslab.ideprobe

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Failure
import scala.util.Try
import scala.util.control.NonFatal

import org.virtuslab.ideprobe.ProbeExtensions.LambdaVisitor

trait ProbeExtensions {

  implicit final class URIExtension(uri: URI) {
    def resolveChild(name: String): URI = {
      if (uri.getPath.endsWith("/")) uri.resolve(name)
      else URI.create(s"$uri/$name")
    }
  }

  implicit final class PathExtension(path: Path) {
    def directChildren(): List[Path] = {
      val stream = Files.list(path)
      try stream.iterator().asScala.toList
      finally stream.close()
    }

    def recursiveChildren(): List[Path] = {
      val stream = Files.walk(path)
      try stream.iterator().asScala.toList
      finally stream.close()
    }

    def recursiveFiles(filter: Path => Boolean = _ => true): List[Path] = {
      val files = mutable.Buffer.empty[Path]
      Files.walkFileTree(
        path,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (filter(file)) {
              files.append(file)
            }
            super.visitFile(file, attrs)
          }
        }
      )
      files.toList
    }

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

    def createTempDirectory(prefix: String): Path = {
      path.resolve(s"$prefix-${UUIDs.randomUUID()}")
    }

    def copyTo(target: Path): Path = {
      Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    }

    def moveTo(target: Path, replace: Boolean = false): Path = {
      target.createParentDirectory()
      if (replace) {
        Files.move(path, target, StandardCopyOption.REPLACE_EXISTING)
      } else {
        Files.move(path, target)
      }
    }

    def write(content: String): Path = {
      path.createParentDirectory()
      Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    def edit(f: String => String): Path = {
      val toEdit = content()
      val edited = f(toEdit)
      write(edited)
    }

    def append(content: InputStream): Path = {
      val out = path.outputStream(append = true)
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

    def outputStream(append: Boolean = false): OutputStream = {
      val output = if (append) {
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
      } else {
        Files.newOutputStream(path)
      }
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
      attributes.add(GROUP_EXECUTE)
      attributes.add(OTHERS_EXECUTE)
      Files.setPosixFilePermissions(path, attributes)
    }

    def makeExecutableRecursively(): Path =
      Files.walkFileTree(path, new LambdaVisitor(path => path.makeExecutable()))

    def delete(): Unit = {
      try Files.deleteIfExists(path)
      catch {
        case _: Exception =>
          val deletingVisitor = new ProbeExtensions.DeletingVisitor(path)
          Files.walkFileTree(path, deletingVisitor)
      }
    }

    def content(): String = {
      new String(Files.readAllBytes(path))
    }

    def lines(): Seq[String] =
      Files.readAllLines(path).asScala.toSeq

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
  private class LambdaVisitor(operation: Path => Unit) extends SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      operation(file)
      FileVisitResult.CONTINUE
    }
  }

  private class DeletingVisitor(root: Path) extends SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (!attrs.isDirectory) Files.delete(file)
      FileVisitResult.CONTINUE
    }

    override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
      exc match {
        case NonFatal(e) =>
          val message = s"[${Thread.currentThread().getId}] Failure while deleting $root at file $file"
          val exception = new IOException(message, e)
          exception.printStackTrace()
          FileVisitResult.CONTINUE
      }
    }

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      val deleted = Option(exc) match {
        case None =>
          Try(Files.delete(dir))
        case Some(e) => Failure(e)
      }
      deleted.failed.foreach { case NonFatal(e) =>
        val message = s"[${Thread.currentThread().getId}] Failure while deleting $root at dir  $dir"
        val exception = new IOException(message, e)
        exception.printStackTrace()
      }
      FileVisitResult.CONTINUE
    }
  }
}
