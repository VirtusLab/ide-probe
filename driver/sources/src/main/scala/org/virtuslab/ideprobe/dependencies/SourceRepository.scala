package org.virtuslab.ideprobe.dependencies

import java.net.URI
import org.virtuslab.ideprobe.ConfigFormat
import pureconfig.{ConfigReader, ConfigWriter}
import pureconfig.generic.semiauto.{deriveReader, deriveWriter}

sealed trait SourceRepository
object SourceRepository extends ConfigFormat {
  final case class Git(path: URI, ref: Option[String]) extends SourceRepository

  implicit val sourceRepositoryReader: ConfigReader[SourceRepository] = possiblyAmbiguousAdtReader[SourceRepository](
    deriveReader[Git]
  )

  implicit val sourceRepositoryWriter: ConfigWriter[SourceRepository] = possiblyAmbiguousAdtWriter[SourceRepository](
    deriveWriter[Git]
  )
}
