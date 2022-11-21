package org.virtuslab.ideprobe.dependencies

trait DependencyResolver[Key] {
  def resolve(key: Key): Dependency
}

object DependencyResolver {
  def apply[Key](f: Key => Dependency): DependencyResolver[Key] = new DependencyResolver[Key] {
    def resolve(key: Key): Dependency = f(key)
  }
}
