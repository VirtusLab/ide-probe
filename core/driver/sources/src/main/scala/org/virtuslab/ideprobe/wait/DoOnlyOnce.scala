package org.virtuslab.ideprobe.wait

class DoOnlyOnce(action: => Unit) {
  private var doneSuccessfully = false

  def attempt(): Unit = {
    if (!doneSuccessfully) {
      try {
        action
        doneSuccessfully = true
      } catch {
        case _: Exception =>
      }
    }
  }
}
