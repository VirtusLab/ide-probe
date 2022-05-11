package org.virtuslab.ideprobe

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.error.ConfigReaderException
import org.virtuslab.ideprobe.Extensions._
import scala.reflect.ClassTag

final case class Config(source: ConfigObjectSource, fallback: Option[Config] = None) {

  def withFallback(config: Config): Config = copy(fallback = Some(config))

  def as[A: ClassTag](implicit reader: ConfigReader[A]): A = {
    getEither[A](None) match {
      case Right(value) => value
      case Left(value)  => throw new ConfigReaderException[A](value)
    }
  }

  def apply[A: ClassTag](path: String)(implicit reader: ConfigReader[A]): A = {
    getEither[A](Some(path)) match {
      case Right(value) => value
      case Left(value)  => throw new ConfigReaderException[A](value)
    }
  }

  def get[A: ClassTag](path: String)(implicit reader: ConfigReader[A]): Option[A] = {
    getEither[A](Some(path)).toOption
  }

  def getOrElse[A: ClassTag](path: String, fallback: => A)(implicit reader: ConfigReader[A]): A = {
    getEither[A](Some(path)).getOrElse(fallback)
  }

  private def getEither[A: ClassTag](
      path: Option[String]
  )(implicit reader: ConfigReader[A]): ConfigReader.Result[A] = {
    path.fold[ConfigSource](source)(source.at).load[A].left.flatMap { errors =>
      fallback match {
        case Some(fallback) => fallback.getEither[A](path)
        case None           => Left(errors)
      }
    }
  }

}

object Config {
  val Empty = new Config(ConfigSource.empty)

  def fromString(str: String) = new Config(ConfigSource.string(str))

  def fromFile(path: Path): Config = {
    new Config(ConfigSource.file(path))
  }

  def fromClasspath(resourcePath: String): Config = {
    new Config(ConfigSource.resources(resourcePath))
  }

  def fromMap(properties: Map[String, String]): Config = {
    new Config(ConfigSource.fromConfig(ConfigFactory.parseMap(properties.asJava)))
  }
}
