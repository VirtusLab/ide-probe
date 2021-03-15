package org.virtuslab.ideprobe.protocol

sealed abstract class TestScope(val module: ModuleRef)

object TestScope {
  case class Module(override val module: ModuleRef) extends TestScope(module)
  case class Directory(override val module: ModuleRef, directoryName: String) extends TestScope(module)
  case class Package(override val module: ModuleRef, packageName: String) extends TestScope(module)
  case class Class(override val module: ModuleRef, className: String) extends TestScope(module)
  case class Method(override val module: ModuleRef, className: String, methodName: String) extends TestScope(module)
}
