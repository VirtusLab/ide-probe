package org.virtuslab

import java.nio.file.Path

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.jps.model.java.JavaResourceRootType.{RESOURCE, TEST_RESOURCE}
import org.jetbrains.jps.model.java.JavaSourceRootType.{SOURCE, TEST_SOURCE}
import org.virtuslab.handlers.VFS
import org.virtuslab.ideprobe.protocol.{ContentRoot, ModuleRef, ProjectRef}
import org.virtuslab.ideprobe.Extensions._

object ProbePluginExtensions {
  final implicit class ModuleExtension(module: Module) {
    def toRef: ModuleRef = {
      val project = module.getProject
      ModuleRef(module.getName, ProjectRef(project.getName))
    }

    def dependencies: Array[Module] = roots.getDependencies

    def contentRoots(kind: ContentRoot): Set[Path] = {
      val rootType = kind match {
        case ContentRoot.MainSources   => SOURCE
        case ContentRoot.MainResources => RESOURCE
        case ContentRoot.TestSources   => TEST_SOURCE
        case ContentRoot.TestResources => TEST_RESOURCE
      }

      roots.getSourceRoots(rootType).asScala.map(VFS.toPath).toSet
    }

    def roots: ModuleRootManager = ModuleRootManager.getInstance(module)
  }
}
