package org.virtuslab.ideprobe.protocol

sealed trait ProjectRef

object ProjectRef {
  case object Default extends ProjectRef
  case class ByName(name: String) extends ProjectRef

  def apply(name: String): ProjectRef = ByName(name)
}
