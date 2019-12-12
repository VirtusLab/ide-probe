package com.virtuslab.ideprobe

object Close {
  def apply(closeables: AutoCloseable*): Unit = {
    closeables.foreach(_.close())
  }
}
