package org.virtuslab.ideprobe

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Collections
import org.virtuslab.ideprobe.Extensions._

sealed trait WorkspaceTemplate {
  def setupIn(workspace: Path): Unit
}

object WorkspaceTemplate {
  def from(config: WorkspaceConfig): WorkspaceTemplate = {
    import org.virtuslab.ideprobe.dependencies.Resource._
    config match {
      case WorkspaceConfig.Default(resource) =>
        resource match {
          case File(path) =>
            FromFile(path)
          case Http(uri) if uri.getHost == "github.com" =>
            FromGit(uri.toString, None)
          case Jar(uri) =>
            FromJar(uri)
          case other =>
            throw new IllegalArgumentException(s"Unsupported workspace template: $other")
        }
      case git: WorkspaceConfig.Git =>
        val repository = git.path
        val ref = Some(git.ref)
        repository match {
          case File(path) =>
            FromGit(path.toString, ref)
          case Http(uri) =>
            FromGit(uri.toString, ref)
          case Unresolved(path, _) =>
            FromGit(path, ref)
          case other =>
            throw new IllegalArgumentException(s"Unsupported git workspace template: $other")
        }
    }
  }

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
      val cloned = Shell.run("git", "clone", repository, workspace.toString)
      if (cloned.exitCode != 0) throw new IllegalStateException(s"Could not clone git $repository")
      ref.foreach { ref =>
        val checkout = Shell.run(in = workspace, "git", "checkout", ref)
        if (checkout.exitCode != 0) throw new IllegalStateException(s"Could not checkout $ref in $repository")
      }
    }
  }
}
