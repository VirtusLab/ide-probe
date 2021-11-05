package org.virtuslab.ideprobe.bazel.protocol

import org.virtuslab.ideprobe.ConfigFormat
import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

case class BazelCommandParams(
    targets: Seq[String] = Nil,
    name: String = "test",
    bazelFlags: Seq[String] = Nil,
    executableFlags: Seq[String] = Nil
)

object BazelCommandParams extends ConfigFormat {
  lazy val format: ConfigConvert[BazelCommandParams] = deriveConvert[BazelCommandParams]
}
