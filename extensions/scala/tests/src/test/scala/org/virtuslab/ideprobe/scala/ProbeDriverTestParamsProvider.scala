package org.virtuslab.ideprobe.scala

import org.junit.runners.Parameterized
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.IntelliJVersion.release
import org.virtuslab.ideprobe.dependencies.Plugin

import java.util

trait ProbeDriverTestParamsProvider {

  private val v212: List[Array[Any]] =
    List(
      Array(Plugin("org.intellij.scala", "2021.2.10"), release("2021.2.1", "212.5080.55"))
    )

  private val v213: List[Array[Any]] =
    List(
      Array(Plugin("org.intellij.scala", "2022.1.15"), release("2022.1.1", "221.5591.52")),
      Array(Plugin("org.intellij.scala", "2021.2.10"), release("2021.2.1", "212.5080.55"))
    )

  @Parameterized.Parameters
  def versions(): util.Collection[Array[Any]] = {
    val result = ScalaVersion(scala.util.Properties.versionNumberString) match {
      case ScalaVersion(_, 12, _) => v212
      case _ => v213
    }
    result.asJavaCollection
  }

  private case class ScalaVersion(major: Int, minor: Int, patch: Int)

  private object ScalaVersion {
    def apply(version: String): ScalaVersion = {
      version.split("\\.") match {
        case Array(major, minor, patch) => ScalaVersion(major.toInt, minor.toInt, patch.toInt)
        case _ => throw new RuntimeException(s"invalid scala version format: $version")
      }
    }
  }

}
