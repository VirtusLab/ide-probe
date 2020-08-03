package org.virtuslab

import java.nio.file.Path

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.jps.model.java.JavaResourceRootType.RESOURCE
import org.jetbrains.jps.model.java.JavaResourceRootType.TEST_RESOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.SOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.TEST_SOURCE
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.virtuslab.handlers.VFS
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.ContentRoots
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.ProjectRef

object ProbePluginExtensions {

  final implicit class ModuleExtension(module: Module) {
    def toRef: ModuleRef = {
      val project = module.getProject
      ModuleRef(module.getName, ProjectRef(project.getName))
    }

    def dependencies: Array[Module] = roots.getDependencies

    def contentRoots: ContentRoots = {
      def by(rootType: JpsModuleSourceRootType[_]): Set[Path] = roots.getSourceRoots(rootType).asScala.map(VFS.toPath).toSet

      ContentRoots(
        sources = by(SOURCE),
        resources = by(RESOURCE),
        testSources = by(TEST_SOURCE),
        testResources = by(TEST_RESOURCE),
      )
    }

    def roots: ModuleRootManager = ModuleRootManager.getInstance(module)
  }

}
