package org.virtuslab.ideprobe

import java.net.ServerSocket

object Network {

  def freePort(): Int = {
    val s = new ServerSocket(0)
    try s.getLocalPort
    finally s.close()
  }

}
