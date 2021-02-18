package org.virtuslab.ideprobe.bazel

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.utils.Keyboard
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe.{Config, RunningIntelliJFixture}
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension

trait BazelOpenProjectFixture extends BazeliskExtension { this: RobotPluginExtension =>

  private implicit class RemoteRobotExt(robot: RemoteRobot) {
    def clickButton(name: String): Unit = {
      robot.find(query.button(name)).doClick()
    }
    def clickRadioButton(name: String): Unit = {
      robot.find(query.radioButton(name)).doClick()
    }
  }

  def openProjectWithBazel(intelliJ: RunningIntelliJFixture): ProjectRef = {
    val robot = intelliJ.probe.withRobot.robot
    val projectView = prepareBazelProjectViewFile(intelliJ.config, intelliJ.workspace)

    // delete previous project
    intelliJ.workspace.resolve(".ijwb").delete()

    // set bazel executable path
    BazelProbeDriver(intelliJ.probe).setupBazelExec(bazelPath(intelliJ.workspace))

    // start project import wizard
    intelliJ.probe.invokeActionAsync("Blaze.ImportProject2")

    // set workspace/repository root path
    robot.find(query.className("TextFieldWithHistory")).setText(intelliJ.workspace.toRealPath().toString)
    robot.clickButton("Next")

    // set project view configuration file
    robot.clickRadioButton("Import project view file")
    robot.find(query.div("name" -> "projectview-file-path-field")).setText(projectView.toString)
    robot.clickButton("Next")

    // complete import wizard
    robot.clickButton("Finish")

    intelliJ.probe.awaitIdle()

    projectView.delete()

    intelliJ.probe.listOpenProjects().head
  }

  private def prepareBazelProjectViewFile(config: Config, workspace: Path): Path = {
    val directories = config[List[String]]("bazel.import.directories")
    val additionalLanguages = config.get[List[String]]("bazel.import.languages").getOrElse(Nil)

    val text =
      s"""${section("directories", directories)}
         |
         |derive_targets_from_directories: true
         |
         |${section("additional_languages", additionalLanguages)}""".stripMargin
    val file = workspace.resolve(s"ide-probe${UUID.randomUUID()}.viewconfig")
    file.write(text)
    workspace.relativize(file)
  }

  private def section(name: String, elements: List[String]): String = {
    if (elements.isEmpty) {
      ""
    } else {
      s"$name:\n${indentStrings(elements)}\n"
    }
  }

  private def indentStrings(strs: List[String]): String = {
    strs.map("  " + _).mkString("\n")
  }
}
