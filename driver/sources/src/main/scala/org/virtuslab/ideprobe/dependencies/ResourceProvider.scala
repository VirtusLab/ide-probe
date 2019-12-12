package org.virtuslab.ideprobe.dependencies

import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.virtuslab.ideprobe.Extensions._

trait ResourceProvider {
  def get(uri: URI, provider: () => InputStream): Resource

  def get(uri: URI): Resource = get(uri, () => uri.toURL.openStream())
}

object ResourceProvider {
  def from(config: DependenciesConfig.ResourceProvider): ResourceProvider = {
    config.cacheDir.map(new Cached(_)).getOrElse(Default)
  }

  val Default = new Cached(Paths.get(sys.props("java.io.tmpdir"), "ideprobe", "cache"))

  final class Cached(directory: Path) extends ResourceProvider {
    override def get(uri: URI, provider: () => InputStream): Resource = {
      Resource.from(uri) match {
        case Resource.Http(uri) =>
          cacheUri(uri, provider, cached => s"Fetching $uri into $cached")
        case Resource.Jar(uri) =>
          cacheUri(uri, provider, cached => s"Extracting $uri from jar into $cached")
        case nonCacheable =>
          nonCacheable
      }
    }

    private def cacheUri(uri: URI, stream: () => InputStream, message: Path => String): Resource.File = {
      val cachedResource = cached(uri)
      if (!cachedResource.isFile) {
        println(message(cachedResource))
        Files
          .createTempFile("cached-resource", "-tmp")
          .append(stream())
          .moveTo(cachedResource)
      }
      Resource.File(cachedResource)
    }

    private def cached(uri: URI): Path = {
      directory.resolve(Hash.md5(uri.toString))
    }
  }
}
