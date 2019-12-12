package com.virtuslab.ideprobe.dependencies
import java.nio.file.Path
import java.nio.file.Paths

trait LibraryProvider[Key] {
  def apply(key: Key, resources: ResourceProvider): Path
}

object LibraryProvider {
  val DefaultDir: Path = Paths.get(sys.props("java.io.tmpdir"), "ideprobe", "libs")
}
