package com.virtuslab.ideprobe.dependencies

import com.virtuslab.ideprobe.dependencies.MavenRepository.Key

final class MavenRepository(uri: String) extends DependencyResolver[Key] {
  override def resolve(key: Key): Dependency = {
    import key._
    val groupPath = group.replace('.', '/')
    Dependency(s"$uri/$groupPath/$artifact/$version/$artifact-$version.zip")
  }
}

object MavenRepository {
  final case class Key(group: String, artifact: String, version: String)
}
