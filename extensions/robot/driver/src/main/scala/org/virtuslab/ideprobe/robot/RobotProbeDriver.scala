package org.virtuslab.ideprobe
package robot

import com.intellij.remoterobot.{RemoteRobot, SearchContext}
import java.nio.file.Path
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotSyntax._
import org.virtuslab.ideprobe.wait.DoOnlyOnce
import scala.collection.mutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object RobotProbeDriver {
  val robotPortProperty = "robot-server.port"

  val pluginId = "com.jetbrains.test.robot-server-plugin"

  private val robotDrivers: mutable.Map[ProbeDriver, RobotProbeDriver] = mutable.Map.empty

  def apply(driver: ProbeDriver): RobotProbeDriver = synchronized {
    robotDrivers.getOrElseUpdate(
      driver, {
        driver.as(
          pluginId, { driver =>
            val port = getRobotPort(driver)
            val url = s"http://127.0.0.1:$port"
            println(s"Robot available at $url")
            val robot = new RemoteRobot(url)
            new RobotProbeDriver(driver, robot)
          }
        )
      }
    )
  }

  private def getRobotPort(driver: ProbeDriver) = {
    driver
      .systemProperties()
      .getOrElse(robotPortProperty, error(s"$robotPortProperty property not set in IntelliJ"))
      .toInt
  }
}

final class RobotProbeDriver(
    driver: ProbeDriver,
    val robot: RemoteRobot
) extends SearchableComponent {

  override protected def searchContext: SearchContext = robot
  override protected def robotTimeout: FiniteDuration = RobotSyntax.robotTimeout

  def extendWaitLogic(waitLogic: WaitLogic): WaitLogic = {
    val closeTip = new DoOnlyOnce(closeTipOfTheDay())
    val hideModal = new DoOnlyOnce(hideImportModalWindow())
    waitLogic.doWhileWaiting {
      hideModal.attempt()
      closeTip.attempt()
      checkBuildPanelErrors()
    }
  }

  def openProject(
      path: Path,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    driver.openProject(path, extendWaitLogic(waitLogic))
  }

  def closeTipOfTheDay(): Unit = {
    robot.mainWindow
      .findWithTimeout(query.dialog("Tip of the Day"), 100.millis)
      .findWithTimeout(query.button("text" -> "Close"), 100.millis)
      .doClick()
  }

  def hideImportModalWindow(): Unit = {
    robot.mainWindow
      .findWithTimeout(query.dialog("name" -> "dialog0"), 100.millis)
      .findWithTimeout(query.button("text" -> "Background"), 100.millis)
      .doClick()
  }

  def checkBuildPanelErrors(): Unit = {
    robot.findOpt(query.className("MultipleBuildsPanel")).foreach { buildPanel =>
      val tree = buildPanel.find(query.className("Tree"))
      val treeTexts = tree.fullTexts
      val hasErrors = treeTexts.contains("failed")
      if (hasErrors) {
        val message = buildPanel
          .find(query.div("accessiblename" -> "Editor", "class" -> "EditorComponentImpl"))
          .fullText
        error(s"Failed to open project. Output: $message")
      }
    }
  }
}
