package org.virtuslab.ideprobe.dependencies

import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.config.{DependenciesConfig, PathsConfig}

trait ResourceProvider {
  def get(uri: URI, provider: => InputStream): Path

  def get(uri: URI): Path = get(uri, uri.toURL.openStream())
}

object ResourceProvider {
  def from(paths: PathsConfig): ResourceProvider = {
    new Cached(paths.cache)
  }

  val Default = new Cached(PathsConfig.Default.cache)

  final class Cached(directory: Path) extends ResourceProvider {
    override def get(uri: URI, provider: => InputStream): Path = {
      Resource.from(uri) match {
        case Resource.Http(uri) =>
          cacheUri(uri, provider, cached => s"Fetching $uri into $cached")
        case Resource.Jar(uri) =>
          cacheUri(uri, provider, cached => s"Extracting $uri from jar into $cached")
        case file: Resource.File =>
          file.path
        case Resource.Unresolved(path, cause) =>
          throw new RuntimeException(s"Could not resolve $path due to $cause")
      }
    }

    private def cacheUri(
        uri: URI,
        stream: => InputStream,
        message: Path => String
    ): Path = {
      val cachedResource = cached(uri)
      if (!cachedResource.isFile) {
        println(message(cachedResource))
        Files
          .createTempFile("cached-resource", "-tmp")
          .append(stream)
          .moveTo(cachedResource)
      }
      cachedResource
    }

    private def cached(uri: URI): Path = {
      directory.resolve(Hash.md5(uri.toString))
    }
  }
}
