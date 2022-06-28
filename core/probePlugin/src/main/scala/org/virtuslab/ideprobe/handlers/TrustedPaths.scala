package org.virtuslab.ideprobe.handlers

import java.nio.file.Path

import com.intellij.openapi.components.ServiceManager

import org.virtuslab.ideprobe.handlers.App.ReflectionOps

object TrustedPaths {
  def add(path: Path): Unit = {
    try {
      val className = Class.forName("com.intellij.ide.impl.TrustedPathsSettings")
      val settings = ServiceManager.getService(className)
      settings.invoke[Unit]("addTrustedPath")(path.toString)
    } catch {
      case _: ClassNotFoundException =>
        println("Trusted paths not supported in this version of IntelliJ")
    }
  }
}
