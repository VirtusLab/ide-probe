package org.virtuslab.ideprobe.dependencies

import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.annotation.tailrec

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbePaths
import org.virtuslab.ideprobe.download.FileDownloader

trait ResourceProvider {
  def get(uri: URI, provider: () => InputStream): Path

  def get(uri: URI): Path = get(uri, () => uri.toURL.openStream())
}

object ResourceProvider {
  def fromConfig(paths: IdeProbePaths, retries: Int): ResourceProvider = {
    new Cached(paths.cache, retries)
  }

  val Default = new Cached(IdeProbePaths.Default.cache, 0)

  final class Cached(directory: Path, retries: Int) extends ResourceProvider {
    override def get(uri: URI, provider: () => InputStream): Path = {
      Resource.from(uri) match {
        case Resource.Http(uri) =>
          cacheUrl(uri, cached => s"Fetching $uri into $cached", retries)
        case Resource.Jar(uri) =>
          cacheJar(uri, provider, cached => s"Extracting $uri from jar into $cached", retries)
        case file: Resource.File if file.path.toFile.exists() =>
          file.path
        case file: Resource.File =>
          throw new RuntimeException(s"Resource ${file.path} does not exist")
        case Resource.Unresolved(path, cause) =>
          throw new RuntimeException(s"Could not resolve $path due to $cause")
      }
    }

    @tailrec
    def retry[T](times: Int)(body: () => T): T = {
      try {
        body()
      } catch {
        case e: Throwable =>
          if (times > 0) {
            Thread.sleep(10000)
            retry(times - 1)(body)
          } else throw e
      }
    }

    private def cacheJar(
        uri: URI,
        createStream: () => InputStream,
        message: Path => String,
        retries: Int
    ): Path = {
      val cachedResource = cached(uri)
      if (!cachedResource.isFile) {
        retry(retries) { () =>
          val stream = createStream()
          println(message(cachedResource))
          Files
            .createTempFile("cached-resource", "-tmp")
            .append(stream)
            .moveTo(cachedResource)
        }
      }
      cachedResource
    }

    private def cacheUrl(
        uri: URI,
        message: Path => String,
        retries: Int
    ): Path = {
      val cachedResource = cached(uri)
      if (!cachedResource.isFile) {
        retry(retries) { () =>
          println(message(cachedResource))
          val tempFile: Path = Files.createTempDirectory("cached-resource")
          val downloader = FileDownloader(tempFile).download(uri.toURL)
          downloader.moveTo(cachedResource)
        }
      }
      cachedResource
    }

    private def cached(uri: URI): Path = {
      val resultPath = directory.resolve(Hash.md5(uri.toString))
      val uriExtensionIndex = uri.toString.lastIndexOf('.')
      if (uriExtensionIndex != -1)
        Paths.get(resultPath.toString + uri.toString.substring(uriExtensionIndex)) // keep extension in cached file name
      else
        resultPath
    }
  }
}
