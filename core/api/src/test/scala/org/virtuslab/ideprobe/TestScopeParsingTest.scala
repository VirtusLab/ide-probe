package org.virtuslab.ideprobe

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.TestScope

@RunWith(classOf[JUnit4])
final class TestScopeParsingTest {

  private def testParsing(confString: String): TestScope =
    Config.fromString(confString).as[TestScope]

  @Test
  def parseModuleWithDefaultProject(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName", project: "default" }}""")
    assertEquals(scope, TestScope.Module(ModuleRef("moduleName")))
  }

  @Test
  def parseModuleWithNamedProject(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName", project: { name: "Name" } }}""")
    assertEquals(scope, TestScope.Module(ModuleRef("moduleName", "Name")))
  }

  @Test
  def parseDirectory(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName" }, directoryName: "/tmp" }""")
    assertEquals(scope, TestScope.Directory(ModuleRef("moduleName"), "/tmp"))
  }

  @Test
  def parsePackage(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName" }, packageName: "Package" }""")
    assertEquals(scope, TestScope.Package(ModuleRef("moduleName"), "Package"))
  }

  @Test
  def parseClass(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName" }, className: "Class" }""")
    assertEquals(scope, TestScope.Class(ModuleRef("moduleName"), "Class"))
  }

  @Test
  def parseMethod(): Unit = {
    val scope = testParsing("""{ module: { name: "moduleName" }, className: "Class", methodName: "Method" }""")
    assertEquals(scope, TestScope.Method(ModuleRef("moduleName"), "Class", "Method"))
  }
}
