package com.intellij.diagnostic


abstract class LogEventException extends Exception {

  def getLogMessage: Any

}
