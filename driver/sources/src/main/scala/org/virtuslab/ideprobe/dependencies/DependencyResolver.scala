package org.virtuslab.ideprobe.dependencies

trait DependencyResolver[Key] {
  def resolve(key: Key): Dependency
}
