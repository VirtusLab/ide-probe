package org.virtuslab.ideprobe

import java.net.URI
import java.nio.file._
import java.util.Collections

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.config.WorkspaceConfig

trait WorkspaceProvider {
  def setup(paths: IdeProbePaths): Path
  def cleanup(path: Path): Unit
}

case class ExistingWorkspace(path: Path) extends WorkspaceProvider {
  override def setup(paths: IdeProbePaths): Path = path
  override def cleanup(path: Path): Unit = ()
}

object WorkspaceProvider {
  def from(config: WorkspaceConfig): WorkspaceProvider = {
    import org.virtuslab.ideprobe.dependencies.Resource._
    config match {
      case WorkspaceConfig.Existing(path) =>
        ExistingWorkspace(path)
      case WorkspaceConfig.Default(resource) =>
        resource match {
          case File(path) =>
            WorkspaceTemplate.FromFile(path)
          case Http(uri) if uri.getHost == "github.com" =>
            WorkspaceTemplate.FromGit(uri.toString, None)
          case Jar(uri) =>
            WorkspaceTemplate.FromJar(uri)
          case other =>
            throw new IllegalArgumentException(s"Unsupported workspace template: $other")
        }
      case git: WorkspaceConfig.Git =>
        val repository = git.path
        val ref = Some(git.ref)
        repository match {
          case File(path) =>
            WorkspaceTemplate.FromGit(path.toString, ref)
          case Http(uri) =>
            WorkspaceTemplate.FromGit(uri.toString, ref)
          case Unresolved(path, _) =>
            WorkspaceTemplate.FromGit(path, ref)
          case other =>
            throw new IllegalArgumentException(s"Unsupported git workspace template: $other")
        }
    }
  }

}

sealed trait WorkspaceTemplate extends WorkspaceProvider {

  override final def setup(paths: IdeProbePaths): Path = {
    val workspaceBase = paths.workspaces.createTempDirectory("ideprobe-workspace")

    val workspace = workspaceBase.createDirectory("ws")
    setupIn(workspace)
    workspace
  }

  override def cleanup(path: Path): Unit = {
    path.delete()
    path.getParent.delete()
  }

  def setupIn(workspace: Path): Unit
}

object WorkspaceTemplate {
  case object Empty extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = ()
  }

  case class FromFile(path: Path) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      path.copyDirContent(workspace)
    }
  }

  case class FromResource(relativePath: String) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      val path = Paths.get(getClass.getResource(s"/$relativePath").getPath).toAbsolutePath
      path.copyDirContent(workspace)
    }
  }

  case class FromJar(uri: URI) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      val pathInJar = uri.toString.split("!")(1)
      val zipFs = FileSystems.newFileSystem(uri, Collections.emptyMap[String, String])
      val rootDirectoryInZip = zipFs.getPath(pathInJar)
      val workspaceFiles = Files.walk(rootDirectoryInZip).skip(1)
      try {
        workspaceFiles.forEach { source =>
          val relativePathInZip = source.toString.stripPrefix(rootDirectoryInZip.toString).stripPrefix("/")
          val target = workspace.resolve(relativePathInZip)
          if (Files.isDirectory(source)) {
            target.createDirectory()
          } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            target.makeExecutable() // jar (zip) does not preserve access rights
          }
        }
      } finally {
        workspaceFiles.close()
        zipFs.close()
      }
    }
  }

  case class FromGit(repository: String, ref: Option[String]) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      import org.virtuslab.ideprobe.dependencies.git.GitHandler._
      val git = repository.clone(workspace)
      ref.foreach{
        ref => git.checkout(ref)
      }
    }
  }
}
