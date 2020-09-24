package org.virtuslab

package object ideprobe {
  def error(msg: String) = throw new RuntimeException(msg)
}
