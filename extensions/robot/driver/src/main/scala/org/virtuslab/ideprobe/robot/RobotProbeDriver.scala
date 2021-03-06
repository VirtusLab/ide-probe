package org.virtuslab.ideprobe
package robot

import com.intellij.remoterobot.{RemoteRobot, SearchContext}
import java.nio.file.Path
import java.time.Duration
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotSyntax._
import scala.collection.mutable
import scala.util.Try

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
  override protected def robotTimeout: Duration = RobotSyntax.robotTimeout

  def openProject(path: Path): ProjectRef = {
    val ref = driver.openProject(path)
    closeTipOfTheDay()
    checkBuildPanelErrors()
    ref
  }

  def closeTipOfTheDay(): Unit = {
    Try(robot.mainWindow.find(query.dialog("Tip of the Day")).button("Close").doClick())
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
        throw new RuntimeException(s"Failed to open project. Output: $message")
      }
    }
  }
}
