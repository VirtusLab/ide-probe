package org.virtuslab.ideprobe.dependencies

import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import scala.annotation.tailrec

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbePaths

trait ResourceProvider {
  def get(uri: URI, provider: () => InputStream): Path

  def get(uri: URI): Path = get(uri, () => uri.toURL.openStream())
}

object ResourceProvider {
  def fromConfig(paths: IdeProbePaths, retries: Int): ResourceProvider = {
    new Cached(paths.cache, retries)
  }

  final class Cached(directory: Path, retries: Int) extends ResourceProvider {
    override def get(uri: URI, provider: () => InputStream): Path = {
      Resource.from(uri) match {
        case Resource.Http(uri) =>
          cacheUri(uri, provider, cached => s"Fetching $uri into $cached", retries)
        case Resource.Jar(uri) =>
          cacheUri(uri, provider, cached => s"Extracting $uri from jar into $cached", retries)
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

    private def cacheUri(
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

    private def cached(uri: URI): Path = {
      directory.resolve(Hash.md5(uri.toString))
    }
  }
}
