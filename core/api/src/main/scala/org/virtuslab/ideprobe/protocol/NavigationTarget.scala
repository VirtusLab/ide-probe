package org.virtuslab.ideprobe.protocol

final case class NavigationQuery(value: String, includeNonProjectItems: Boolean = false, project: ProjectRef = ProjectRef.Default)
final case class NavigationTarget(name: String, location: String)
