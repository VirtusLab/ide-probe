package org.virtuslab.ideprobe.dependencies

import java.nio.file.Paths

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory


@RunWith(classOf[JUnit4])
final class IntelliJFactoryTest {
  @Test
  def test: Unit = {
    val resourcePath = Paths.get(getClass.getResource(".").getPath)
    val IntelliJVersion(build, release) = IntelliJFactory.version(resourcePath)

    val version = "2020.2.4"
    val buildNumber = "202.8194.7"

    assertEquals(build, version)
    assertEquals(Some(buildNumber), release)
  }
}
