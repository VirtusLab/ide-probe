package org.virtuslab.ideprobe.protocol

sealed trait ShortenCommandLine

object ShortenCommandLine {
  object None extends ShortenCommandLine
  object Manifest extends ShortenCommandLine
  object ClasspathFile extends ShortenCommandLine
  object ArgsFile extends ShortenCommandLine
}
