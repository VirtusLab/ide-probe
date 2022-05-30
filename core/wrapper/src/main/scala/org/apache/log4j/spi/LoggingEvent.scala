package org.apache.log4j.spi

import java.util.logging.Level

class LoggingEvent {

  def getLevel(): Level = null

  def getMessage: Any = null

  def getThrowableInformation: ThrowableInformation = null

  abstract class ThrowableInformation {
    def getThrowable: Throwable
  }
}
