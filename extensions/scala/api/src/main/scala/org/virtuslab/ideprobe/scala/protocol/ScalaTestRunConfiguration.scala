package org.virtuslab.ideprobe.scala.protocol

import org.virtuslab.ideprobe.protocol.ModuleRef

sealed abstract class ScalaTestRunConfiguration(val module: ModuleRef)

case class ScalaTestModuleRunConfiguration(override val module: ModuleRef)
  extends ScalaTestRunConfiguration(module)
case class ScalaTestPackageRunConfiguration(override val module: ModuleRef, packageName: String)
  extends ScalaTestRunConfiguration(module)
case class ScalaTestClassRunConfiguration(override val module: ModuleRef, className: String)
  extends ScalaTestRunConfiguration(module)
case class ScalaTestMethodRunConfiguration(override val module: ModuleRef, className: String, methodName: String)
  extends ScalaTestRunConfiguration(module)
