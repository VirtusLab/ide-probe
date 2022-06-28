package org.virtuslab.ideprobe.bazel.protocol

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.ConfigFormat

case class BazelCommandParams(
    targets: Seq[String] = Nil,
    name: String = "test",
    bazelFlags: Seq[String] = Nil,
    executableFlags: Seq[String] = Nil
)

object BazelCommandParams extends ConfigFormat {
  lazy val format: ConfigConvert[BazelCommandParams] = deriveConvert[BazelCommandParams]
}
