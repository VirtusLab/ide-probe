package org.virtuslab.ideprobe.scala.protocol

import org.virtuslab.ideprobe.protocol.ModuleRef

sealed abstract class ScalaTestRunConfiguration(val module: ModuleRef)

object ScalaTestRunConfiguration {
  case class Module(override val module: ModuleRef) extends ScalaTestRunConfiguration(module)
  case class Package(override val module: ModuleRef, packageName: String) extends ScalaTestRunConfiguration(module)
  case class Class(override val module: ModuleRef, className: String) extends ScalaTestRunConfiguration(module)
  case class Method(override val module: ModuleRef, className: String, methodName: String)
      extends ScalaTestRunConfiguration(module)
}
