package org.virtuslab.ideprobe.dependencies

import java.nio.file.Paths

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
final class IntelliJVersionResolverTest {
  @Test
  def shouldBeAbleToResolveIntelliJVersionFromProductInfo(): Unit = {
    val resourcePath = Paths.get(getClass.getResource("/intellij/productInfo").getPath)

    val IntelliJVersion(build, release, _) = IntelliJVersionResolver.version(resourcePath)

    val releaseVersion = "2020.2.4"
    val buildNumber = "202.8194.7"

    assertEquals(build, buildNumber)
    assertEquals(Some(releaseVersion), release)
  }
}
