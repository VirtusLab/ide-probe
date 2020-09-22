package org.virtuslab.ideprobe.protocol

final case class ApplicationRunConfiguration(
    module: ModuleRef,
    mainClass: String
)

final case class JUnitRunConfiguration(
    module: ModuleRef,
    mainClass: Option[String],
    methodName: Option[String],
    packageName: Option[String],
    directory: Option[String]
)

object JUnitRunConfiguration {
  def mainClass(module: ModuleRef, mainClass: String): JUnitRunConfiguration =
    JUnitRunConfiguration(module, Some(mainClass), None, None, None)

  def method(module: ModuleRef, mainClass: String, methodName: String): JUnitRunConfiguration =
    JUnitRunConfiguration(module, Some(mainClass), Some(methodName), None, None)

  def packageName(module: ModuleRef, packageName: String): JUnitRunConfiguration =
    JUnitRunConfiguration(module, None, None, Some(packageName), None)

  def directory(module: ModuleRef, directory: String): JUnitRunConfiguration =
    JUnitRunConfiguration(module, None, None, None, Some(directory))

  def module(module: ModuleRef): JUnitRunConfiguration =
    JUnitRunConfiguration(module, None, None, None, None)
}

final case class ExpandMacroData(
    fileRef: FileRef,
    macroText: String
)

final case class TestRunConfiguration(module: ModuleRef, runnerNameFragment: Option[String])

object TestRunConfiguration {
  def module(module: ModuleRef): TestRunConfiguration =
    TestRunConfiguration(module, None)

  def module(module: ModuleRef, runnerNameFragment: String): TestRunConfiguration =
    TestRunConfiguration(module, Some(runnerNameFragment))
}
