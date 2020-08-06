package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

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

case class ContentRoots(
  sources: Set[Path],
  resources: Set[Path],
  testSources: Set[Path],
  testResources: Set[Path]
) {
  def allSources: Set[Path] = sources ++ testSources
  def allResources: Set[Path] = resources ++ testResources
  def all: Set[Path] = allSources ++ allResources
}
