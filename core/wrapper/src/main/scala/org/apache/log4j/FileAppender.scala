package org.apache.log4j

abstract class FileAppender extends  {

  def setImmediateFlush(immediateFlush: Boolean) : Unit

  def getImmediateFlush: Boolean

}
