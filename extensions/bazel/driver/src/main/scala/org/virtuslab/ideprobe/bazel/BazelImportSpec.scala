package org.virtuslab.ideprobe.bazel

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.ConfigFormat

case class BazelImportSpec(
    directories: Seq[String] = Nil,
    languages: Seq[String] = Nil
)

object BazelImportSpec extends ConfigFormat {
  implicit val format: ConfigConvert[BazelImportSpec] = deriveConvert[BazelImportSpec]
}
