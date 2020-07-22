package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.jsonrpc.PayloadJsonFormat._
import org.virtuslab.ideprobe.protocol.ContentRoot.{MainResources, MainSources, TestResources, TestSources}
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigWriter}

case class Project(
    name: String,
    basePath: String,
    modules: Seq[Module]
)

case class Module(name: String,
                  contentRoots: Map[ContentRoot, Set[Path]],
                  dependencies: Set[ModuleRef],
                  kind: Option[String])

object Module extends ConfigFormat {
  implicit val reader: ConfigReader[Module] = exportReader[Module].instance
  implicit val writer: ConfigWriter[Module] = exportWriter[Module].instance

  import pureconfig._
  import pureconfig.configurable._

  implicit val contentRootsReader: ConfigReader[Map[ContentRoot, Set[Path]]] =
    genericMapReader[ContentRoot, Set[Path]] {
      case "MainSources"   => Right(MainSources)
      case "MainResources" => Right(MainResources)
      case "TestSources"   => Right(TestSources)
      case "TestResources" => Right(TestResources)
      case other           => Left(CannotConvert(other, ContentRoot.getClass.getSimpleName, "Not supported"))
    }

  implicit val contentRootsWriter: ConfigWriter[Map[ContentRoot, Set[Path]]] =
    genericMapWriter[ContentRoot, Set[Path]](_.toString)

}

sealed trait ContentRoot
object ContentRoot extends ConfigFormat {
  case object MainSources extends ContentRoot
  case object MainResources extends ContentRoot
  case object TestSources extends ContentRoot
  case object TestResources extends ContentRoot

  val All = Seq(MainSources, MainResources, TestSources, TestResources)
}
