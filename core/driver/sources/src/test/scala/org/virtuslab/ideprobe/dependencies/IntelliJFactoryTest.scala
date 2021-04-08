package org.virtuslab.ideprobe.dependencies

import java.io.PrintWriter
import java.nio.file.{Files, Path, Paths}

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory

import scala.io.Source

@RunWith(classOf[JUnit4])
final class IntelliJFactoryTest {


  @Test
  def test: Unit = {
    val resourcePath = Paths.get(getClass.getResource(".").getPath)
    val IntelliJVersion(build, release) = IntelliJFactory.version(resourcePath)

    val version = "2020.1"
    val buildNumber = "201.6668.121"

    assertEquals(build, version)
    assertEquals(Some(buildNumber), release)
  }
}
