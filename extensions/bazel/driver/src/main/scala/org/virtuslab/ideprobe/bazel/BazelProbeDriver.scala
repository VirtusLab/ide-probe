package org.virtuslab.ideprobe.bazel

import com.intellij.remoterobot.RemoteRobot
import java.nio.file.Path
import java.util.UUID
import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.bazel.protocol.BazelEndpoints
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.RobotSyntax._
import org.virtuslab.ideprobe.wait.WaitLogicFactory
import scala.concurrent.duration._

object BazelProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.bazel"

  def apply(driver: ProbeDriver): BazelProbeDriver = {
    driver.as(pluginId, new BazelProbeDriver(_))
  }
}

class BazelProbeDriver(val driver: ProbeDriver) {
  private val robotDriver = RobotProbeDriver(driver)

  def setupBazelExec(path: Path): Unit = {
    driver.send(BazelEndpoints.SetupBazelExecutable, path)
  }

  def buildBazelProject(
      checkFrequency: FiniteDuration = 1.second,
      waitLimit: FiniteDuration = WaitLogicFactory.DefaultAtMost
  ): BazelBuildResult = {
    val robot = robotDriver.robot

    def findBuildResult(): Option[BazelBuildResult] = {
      val success = "Build completed successfully"
      val failure = "Build did NOT complete successfully"
      val messagesToFind = Set(success, failure)
      val consoles = robot.findAll(query.className("EditorComponentImpl"))
      val console = consoles.find { console =>
        messagesToFind.exists { message =>
          val text = console.fullText
          text.contains(message)
        }
      }
      console.map { console =>
        val text = console.fullText
        BazelBuildResult(isSuccessful = text.contains(success), output = text)
      }
    }

    if (robot.findAll(query.className("BaseLabel", "text" -> "Bazel Console")).isEmpty) {
      robot.find(query.className("StripeButton", "text" -> "Bazel Console")).doClick()
    }

    driver.invokeActionAsync("MakeBlazeProject")

    var result = Option.empty[BazelBuildResult]
    val buildWait = WaitLogic.basic(checkFrequency = checkFrequency, atMost = waitLimit) {
      findBuildResult() match {
        case buildResult @ Some(_) =>
          result = buildResult
          WaitDecision.Done
        case None => WaitDecision.KeepWaiting("Waiting for bazel build to complete")
      }
    }

    driver.await(buildWait)

    result.getOrElse(error("Failed to find build result"))
  }

  def importProject(
      importSpec: BazelImportSpec,
      workspace: Path,
      waitLogic: WaitLogic
  ): ProjectRef = {
    val robot = robotDriver.robot
    val projectView = prepareBazelProjectViewFile(importSpec, workspace)

    // delete previous project
    workspace.resolve(".ijwb").delete()

    // start project import wizard
    driver.invokeActionAsync("Blaze.ImportProject2")

    // set workspace/repository root path
    robot
      .find(query.className("TextFieldWithHistory"))
      .setText(workspace.toRealPath().toString)
    robot.clickButton("Next")

    // set project view configuration file
    robot.clickRadioButton("Import project view file")
    robot.find(query.div("name" -> "projectview-file-path-field")).setText(projectView.toString)
    robot.clickButton("Next")

    // complete import wizard
    robot.clickButton("Finish")

    driver.await(waitLogic)

    driver.listOpenProjects().head
  }

  private def prepareBazelProjectViewFile(importSpec: BazelImportSpec, workspace: Path): Path = {
    val text =
      s"""${section("directories", importSpec.directories)}
         |
         |derive_targets_from_directories: true
         |
         |${section("additional_languages", importSpec.languages)}""".stripMargin
    val file = workspace.resolve(s"ide-probe${UUID.randomUUID()}.viewconfig")
    file.write(text)
    file.toFile.deleteOnExit()
    workspace.relativize(file)
  }

  private def section(name: String, elements: Seq[String]): String = {
    if (elements.isEmpty) {
      ""
    } else {
      s"$name:\n${indentStrings(elements)}\n"
    }
  }

  private def indentStrings(strs: Seq[String]): String = {
    strs.map("  " + _).mkString("\n")
  }

  private implicit class RemoteRobotExt(robot: RemoteRobot) {
    def clickButton(name: String): Unit = {
      robot.find(query.button(name)).doClick()
    }
    def clickRadioButton(name: String): Unit = {
      robot.find(query.radioButton(name)).doClick()
    }
  }

}
