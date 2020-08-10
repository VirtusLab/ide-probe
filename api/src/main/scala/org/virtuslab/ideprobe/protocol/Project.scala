package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

import org.virtuslab.ideprobe.protocol.SourceFolder.Kind

case class Project(
    name: String,
    basePath: String,
    modules: Seq[Module]
) {
  def moduleNames: Seq[String] = modules.map(_.name)
  def moduleRefs: Seq[ModuleRef] = modules.map(module => ModuleRef(module.name, ProjectRef(name)))
  def modulesByNames(names: Set[String]): Seq[Module] = modules.filter(m => names.contains(m.name))
}

case class Module(name: String,
                  contentRoots: ContentRoots,
                  dependencies: Set[ModuleRef],
                  kind: Option[String])

case class ContentEntry(path: Option[Path], sourceRoots: Set[SourceFolder], excluded: Set[Path])

case class ContentRoots(entries: Set[ContentEntry]) { self =>
  def byKinds(kinds: SourceFolder.Kind*): Set[SourceFolder] = entries.flatMap(e => e.sourceRoots.filter(s => kinds.contains(s.kind)))

  def sources: Set[SourceFolder] = byKinds(Kind.sources)
  def resources: Set[SourceFolder] = byKinds(Kind.resources)
  def testSources: Set[SourceFolder] = byKinds(Kind.testSources)
  def testResources: Set[SourceFolder] = byKinds(Kind.testResources)

  def allSources: Set[SourceFolder] = sources ++ testSources
  def allResources: Set[SourceFolder] = resources ++ testResources

  def all: Set[SourceFolder] = entries.flatMap(_.sourceRoots)

  def excludePaths: Set[Path] = entries.flatMap(_.excluded)

  def contentEntryRoots: Set[Path] = entries.flatMap(_.path)

  object paths {
    def byKinds(kinds: SourceFolder.Kind*): Set[Path] = self.byKinds(kinds: _*).map(_.path)
    def sources: Set[Path] = byKinds(Kind.sources)
    def resources: Set[Path] = byKinds(Kind.resources)
    def testSources: Set[Path] = byKinds(Kind.testSources)
    def testResources: Set[Path] = byKinds(Kind.testResources)
    def allSources: Set[Path] = sources ++ testSources
    def allResources: Set[Path] = resources ++ testResources
    def all: Set[Path] = self.all.map(_.path)
  }

}

case class SourceFolder(path: Path, packagePrefix: Option[String], kind: SourceFolder.Kind, isGenerated: Boolean)

object SourceFolder {
  type Kind = String
  object Kind {
    val sources = "sources"
    val resources = "resources"
    val testSources = "testSources"
    val testResources = "testResources"
  }
}
