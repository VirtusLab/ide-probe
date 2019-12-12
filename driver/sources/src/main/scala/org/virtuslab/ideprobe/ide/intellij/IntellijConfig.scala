package org.virtuslab.ideprobe.ide.intellij

import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.Plugin

case class IntellijConfig(version: IntelliJVersion = IntelliJVersion.Latest, plugins: Seq[Plugin] = Seq.empty)
