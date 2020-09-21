package org.virtuslab.ideprobe.protocol

final case class ApplicationRunConfiguration(
    module: ModuleRef,
    mainClass: String
)

final case class ExpandMacroData(
    fileRef: FileRef,
    macroText: String
)

sealed abstract class JUnitRunConfiguration(val moduleRef: ModuleRef)

case class ModuleJUnitRunConfiguration(override val moduleRef: ModuleRef)
  extends JUnitRunConfiguration(moduleRef)
case class DirectoryJUnitRunConfiguration(override val moduleRef: ModuleRef, directoryName: String)
  extends JUnitRunConfiguration(moduleRef)
case class PackageJUnitRunConfiguration(override val moduleRef: ModuleRef, packageName: String)
  extends JUnitRunConfiguration(moduleRef)
case class ClassJUnitRunConfiguration(override val moduleRef: ModuleRef, className: String)
  extends JUnitRunConfiguration(moduleRef)
case class MethodJUnitRunConfiguration(override val moduleRef: ModuleRef, className: String, testName: String)
  extends JUnitRunConfiguration(moduleRef)

sealed abstract class TestRunConfiguration(val moduleRef: ModuleRef, val runnerNameFragment: Option[String])

case class ModuleTestRunConfiguration(override val moduleRef: ModuleRef, override val runnerNameFragment: Option[String])
  extends TestRunConfiguration(moduleRef, runnerNameFragment)
case class DirectoryTestRunConfiguration(override val moduleRef: ModuleRef, directoryName: String, override val runnerNameFragment: Option[String])
  extends TestRunConfiguration(moduleRef, runnerNameFragment)
case class PackageTestRunConfiguration(override val moduleRef: ModuleRef, packageName: String, override val runnerNameFragment: Option[String])
  extends TestRunConfiguration(moduleRef, runnerNameFragment)
case class ClassTestRunConfiguration(override val moduleRef: ModuleRef, className: String, override val runnerNameFragment: Option[String])
  extends TestRunConfiguration(moduleRef, runnerNameFragment)
case class MethodTestRunConfiguration(override val moduleRef: ModuleRef, className: String, methodName: String, override val runnerNameFragment: Option[String])
  extends TestRunConfiguration(moduleRef, runnerNameFragment)
