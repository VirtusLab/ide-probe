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

final case class TestRunConfiguration(
   module: ModuleRef,
   packageName: Option[String],
   className: Option[String],
   methodName: Option[String],
   name: Option[String]
)

object TestRunConfiguration {
  def module(module: ModuleRef, runnerNameFragment: Option[String] = None): TestRunConfiguration =
    TestRunConfiguration(module, None, None, None, runnerNameFragment)

  def mainPackage(module: ModuleRef, packageName: String, runnerFragmentName: Option[String] = None): TestRunConfiguration =
    TestRunConfiguration(module, Some(packageName), None, None, runnerFragmentName)

  def mainClass(module: ModuleRef, className: String, runnerNameFragment: Option[String] = None): TestRunConfiguration =
    TestRunConfiguration(module, None, Some(className), None, runnerNameFragment)

  def method(module: ModuleRef, className: String, methodName: String, runnerNameFragment: Option[String] = None): TestRunConfiguration =
    TestRunConfiguration(module, None, Some(className), Some(methodName), runnerNameFragment)
}
