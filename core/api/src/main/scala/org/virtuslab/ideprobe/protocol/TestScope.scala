package org.virtuslab.ideprobe.protocol

import org.virtuslab.ideprobe.ConfigFormat
import pureconfig.{ConfigReader, ConfigWriter}
import pureconfig.generic.semiauto.{deriveReader, deriveWriter}

sealed abstract class TestScope(val module: ModuleRef)

object TestScope extends ConfigFormat {
  case class Module(override val module: ModuleRef) extends TestScope(module)
  case class Directory(override val module: ModuleRef, directoryName: String) extends TestScope(module)
  case class Package(override val module: ModuleRef, packageName: String) extends TestScope(module)
  case class Class(override val module: ModuleRef, className: String) extends TestScope(module)
  case class Method(override val module: ModuleRef, className: String, methodName: String) extends TestScope(module)

  implicit val testScopeConfigReader: ConfigReader[TestScope] = {
    possiblyAmbiguousAdtReader[TestScope](
      deriveReader[Method],
      deriveReader[Class],
      deriveReader[Package],
      deriveReader[Directory],
      deriveReader[Module]
    )
  }

  implicit val testScopeConfigWriter: ConfigWriter[TestScope] = {
    possiblyAmbiguousAdtWriter[TestScope](
      deriveWriter[Method],
      deriveWriter[Class],
      deriveWriter[Package],
      deriveWriter[Directory],
      deriveWriter[Module]
    )
  }
}
