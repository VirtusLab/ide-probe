package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.ConfigFormat
import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

case class BazelImportSpec(
    directories: Seq[String] = Nil,
    languages: Seq[String] = Nil
)

object BazelImportSpec extends ConfigFormat {
  implicit val format: ConfigConvert[BazelImportSpec] = deriveConvert[BazelImportSpec]
}
