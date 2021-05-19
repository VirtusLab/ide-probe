package org.virtuslab.ideprobe.config

import pureconfig.ConfigReader
import pureconfig.generic.auto._
import pureconfig.generic.ProductHint

import java.nio.file.Path

object IdeaProperties {
  case class IdeaProperties(idea: Idea = Idea(), java: Java = Java())

  case class Idea(
    config: IdeaConfig = IdeaConfig(),
    system: IdeaSystem = IdeaSystem(),
    plugins: IdeaPlugins = IdeaPlugins(),
    log: IdeaLog = IdeaLog()
  )
  case class IdeaConfig(path: Option[Path] = None)
  case class IdeaSystem(path: Option[Path] = None)
  case class IdeaPlugins(path: Option[Path] = None)
  case class IdeaLog(path: Option[Path] = None)
  case class Java(util: JavaUtil = JavaUtil())
  case class JavaUtil(prefs: JavaPrefs = JavaPrefs())
  case class JavaPrefs(userRoot: Option[Path] = None)

  implicit val format: ConfigReader[IdeaProperties] = exportReader[IdeaProperties].instance
  implicit val identityHint: ProductHint[JavaPrefs] = ProductHint[JavaPrefs](identity)
}
