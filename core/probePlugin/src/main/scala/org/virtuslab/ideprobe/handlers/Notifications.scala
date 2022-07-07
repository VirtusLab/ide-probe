package org.virtuslab.ideprobe.handlers

import scala.concurrent.Promise
import scala.concurrent.duration.Duration

import com.intellij.notification.Notification
import com.intellij.notification.{Notifications => NotificationListener}
import com.intellij.openapi.application.ApplicationManager

import org.virtuslab.ideprobe.protocol.IdeNotification

object Notifications extends IntelliJApi {
  def await(id: String, duration: Duration): IdeNotification = {
    val result = Promise[IdeNotification]()

    ApplicationManager.getApplication.getMessageBus
      .connect()
      .subscribe(
        NotificationListener.TOPIC,
        new NotificationListener {
          override def notify(notification: Notification): Unit = {
            if (notification.getTitle == id) {
              result.trySuccess(new IdeNotification(notification.getType.name()))
            }
          }
        }
      )
    await(result.future, duration)
  }
}
