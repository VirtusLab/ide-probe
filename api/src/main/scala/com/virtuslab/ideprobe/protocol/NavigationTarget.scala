package com.virtuslab.ideprobe.protocol

final case class NavigationQuery(project: ProjectRef = ProjectRef.Default, value: String)
final case class NavigationTarget(name: String, location: String)
