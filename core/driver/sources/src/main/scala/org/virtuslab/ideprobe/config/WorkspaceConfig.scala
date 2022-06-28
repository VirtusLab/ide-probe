package org.virtuslab.ideprobe.config

import java.nio.file.Path

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.dependencies.Resource

sealed trait WorkspaceConfig

object WorkspaceConfig extends ConfigFormat {

  sealed trait Git extends WorkspaceConfig {
    def path: Resource
    def ref: String
  }

  case class GitBranch(path: Resource, branch: String) extends Git {
    def ref: String = branch
  }
  case class GitTag(path: Resource, tag: String) extends Git {
    def ref: String = tag
  }
  case class GitCommit(path: Resource, commit: String) extends Git {
    def ref: String = commit
  }

  case class Default(path: Resource) extends WorkspaceConfig

  case class Existing(existing: Path) extends WorkspaceConfig

  implicit val workspaceConfigReader: ConfigReader[WorkspaceConfig] = {
    possiblyAmbiguousAdtReader[WorkspaceConfig](
      deriveReader[GitBranch],
      deriveReader[GitTag],
      deriveReader[GitCommit],
      deriveReader[Default],
      deriveReader[Existing]
    )
  }
}
