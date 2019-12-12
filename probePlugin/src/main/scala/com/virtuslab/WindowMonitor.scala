package com.virtuslab

import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import com.intellij.openapi.wm.IdeFrame
import com.virtuslab.handlers.IntelliJApi
import javax.swing
import scala.util.Try

class WindowMonitor extends AWTEventListener {
  import WindowMonitor._

  def eventDispatched(event: AWTEvent): Unit = event match {
    case e: WindowEvent =>
      val window = e.getWindow
      val title = window.title

      Screenshot.take()

      event.getID match {
        case WindowEvent.WINDOW_OPENED =>
          if (isExpected(window)) println(s"opened $title")
          else {
            println(s"opened $title:[$window]") // give more info when opened unexpected window
            // CurrentRequest.fail(new Exception(s"Unexpectedly opened window: [$title]"))
          }
        case WindowEvent.WINDOW_CLOSED =>
          println(s"closed $title")
        case _ => ()
      }
    case _ => ()
  }
}

object WindowMonitor extends IntelliJApi {
  private val monitor = new WindowMonitor

  def inject(): Unit = {
    Toolkit.getDefaultToolkit.addAWTEventListener(monitor, AWTEvent.WINDOW_EVENT_MASK);
  }

  private def isExpected(w: Window): Boolean = w match {
    case _: IdeFrame      => true
    case _: swing.JWindow => true // might lead to false positives but sometimes there is nothing else we know
    case _                => expectedWindows.contains(w.title)
  }

  private val expectedWindows = List("Welcome to IntelliJ IDEA", "Tip of the Day")

  private final implicit class WindowOps(w: Window) {
    def title: String = {
      Try(w.invoke[String]("getTitle")())
        .filter(_.nonEmpty)
        .getOrElse(w.getClass.getName)
    }
  }
}
