package com.virtuslab.ideprobe.ide.intellij

import com.virtuslab.ideprobe.dependencies.IntelliJVersion
import com.virtuslab.ideprobe.dependencies.Plugin

case class IntellijConfig(version: IntelliJVersion = IntelliJVersion.Latest, plugins: Seq[Plugin] = Seq.empty)
