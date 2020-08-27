package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.jps.model.java.JavaResourceRootType.RESOURCE
import org.jetbrains.jps.model.java.JavaResourceRootType.TEST_RESOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.SOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.TEST_SOURCE
import org.virtuslab.ideprobe.protocol.ContentEntry
import org.virtuslab.ideprobe.protocol.ContentRoots
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.Sdk
import org.virtuslab.ideprobe.protocol.SourceFolder

object Modules extends IntelliJApi {
  def resolve(module: ModuleRef): Module = {
    val project = Projects.resolve(module.project)
    val modules = ModuleManager.getInstance(project).getModules
    modules.find(_.getName == module.name) match {
      case Some(module) =>
        module
      case None =>
        val helpMessage =
          if (modules.isEmpty) "There are no open modules"
          else s"Available modules are: ${modules.map(_.getName).mkString(",")}"

        error(s"Could not find module [${module.name}] inside project [${module.name}]. $helpMessage")
    }
  }

  def sdk(moduleRef: ModuleRef): Option[Sdk] = {
    val module = resolve(moduleRef)
    val sdk = ModuleRootManager.getInstance(module).getSdk
    Option(sdk).map(Sdks.convert)
  }

  def dependencies(module: Module): Array[Module] = moduleRootManager(module).getDependencies

  def contentRoots(module: Module): ContentRoots = {
    val entries = moduleRootManager(module).getContentEntries.map { entry =>
      val sourceFolders = entry.getSourceFolders.map { sf =>
        val path = VFS.toPath(sf.getFile)
        val packagePrefix = Option(sf.getPackagePrefix).filterNot(_.isEmpty)
        val kind = sf.getRootType match {
          case SOURCE => SourceFolder.Kind.sources
          case RESOURCE => SourceFolder.Kind.resources
          case TEST_SOURCE => SourceFolder.Kind.testSources
          case TEST_RESOURCE => SourceFolder.Kind.testResources
          case other => other.toString
        }
        val isGenerated = sf.invoke[Boolean]("isForGeneratedSources")()
        SourceFolder(path, packagePrefix, kind, isGenerated)
      }
      ContentEntry(
        Option(entry.getFile).map(VFS.toPath),
        sourceFolders.toSet,
        entry.getExcludeFolderFiles.map(VFS.toPath).toSet
      )
    }
    ContentRoots(entries.toSet)
  }

  private def moduleRootManager(module: Module): ModuleRootManager = ModuleRootManager.getInstance(module)
}
