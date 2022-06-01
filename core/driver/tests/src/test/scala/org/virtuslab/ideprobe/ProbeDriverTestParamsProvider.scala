package org.virtuslab.ideprobe

import org.junit.runners.Parameterized
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.IntelliJVersion.release
import org.virtuslab.ideprobe.dependencies.Plugin

import java.util

trait ProbeDriverTestParamsProvider {

  private val v: List[Array[Any]] =
    List(
      Array(Plugin("org.intellij.scala", "2022.1.15"), release("2022.1.1", "221.5591.52")),
      Array(Plugin("org.intellij.scala", "2021.2.10"), release("2021.2.1", "212.5080.55"))
    )

  @Parameterized.Parameters
  def versions(): util.Collection[Array[Any]] = v.asJavaCollection

}
