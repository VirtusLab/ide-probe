package com.virtuslab.ideprobe.ide.intellij

sealed trait Display
object Display {
  private val Default = Native

  val Mode: Display = {
    Option(System.getenv("IDEPROBE_DISPLAY"))
      .map(fromName)
      .getOrElse(Default)
  }

  val XvfbDisplayId = "7" // doesn't really matter as long as it is not 0

  println(s"Display mode: $Mode")

  private def fromName(name: String): Display = {
    name.toLowerCase() match {
      case "xvfb"   => Xvfb
      case "native" => Native
      case other    => throw new IllegalArgumentException(s"Unsupported display mode: $other")
    }
  }

  case object Xvfb extends Display
  case object Native extends Display
}
