package org.virtuslab.ideprobe.protocol

final case class ApplicationRunConfiguration(
    module: ModuleRef,
    mainClass: String
)

final case class ExpandMacroData(
    fileRef: FileRef,
    macroText: String
)

sealed abstract class TestRunConfiguration(val module: ModuleRef)

object TestRunConfiguration {
  case class Module(override val module: ModuleRef) extends TestRunConfiguration(module)
  case class Directory(override val module: ModuleRef, directoryName: String) extends TestRunConfiguration(module)
  case class Package(override val module: ModuleRef, packageName: String) extends TestRunConfiguration(module)
  case class Class(override val module: ModuleRef, className: String) extends TestRunConfiguration(module)
  case class Method(override val module: ModuleRef, className: String, methodName: String) extends TestRunConfiguration(module)
}

case class TestRunConfigurationMatch(runConfiguration: TestRunConfiguration, runnerNameFragment: Option[String])
