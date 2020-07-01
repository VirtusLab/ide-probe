package org.virtuslab.ideprobe

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.error.ConfigReaderException

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

final case class Config(source: ConfigSource, fallback: Option[Config] = None) {

  def withFallback(config: Config): Config = copy(fallback = Some(config))

  def apply[A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): A = {
    getEither[A](path) match {
      case Right(value) => value
      case Left(value) => throw new ConfigReaderException[A](value)
    }
  }

  def get[A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): Option[A] = {
    getEither[A](path).toOption
  }

  private def getEither[A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): ConfigReader.Result[A] = {
    source.at(path).load[A].left.flatMap { errors =>
      fallback match {
        case Some(fallback) => fallback.getEither[A](path)
        case None => Left(errors)
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
