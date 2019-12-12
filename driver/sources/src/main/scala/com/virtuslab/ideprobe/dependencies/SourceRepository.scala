package com.virtuslab.ideprobe.dependencies

import java.net.URI
import com.virtuslab.ideprobe.ConfigFormat
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

sealed trait SourceRepository
object SourceRepository extends ConfigFormat {
  final case class Git(path: URI, branch: Option[String]) extends SourceRepository

  implicit val pluginReader: ConfigReader[SourceRepository] = possiblyAmbiguousAdtReader[SourceRepository](
    deriveReader[Git]
  )
}
