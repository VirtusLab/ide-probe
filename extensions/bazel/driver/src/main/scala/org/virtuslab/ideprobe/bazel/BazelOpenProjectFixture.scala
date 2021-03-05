package org.virtuslab.ideprobe.bazel

import com.intellij.remoterobot.RemoteRobot
import java.nio.file.Path
import java.util.UUID
import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.{RunningIntelliJFixture, WaitLogic}

trait BazelOpenProjectFixture extends BazeliskExtension { this: RobotPluginExtension =>

  private implicit class RemoteRobotExt(robot: RemoteRobot) {
    def clickButton(name: String): Unit = {
      robot.find(query.button(name)).doClick()
    }
    def clickRadioButton(name: String): Unit = {
      robot.find(query.radioButton(name)).doClick()
    }
  }

  def openProjectWithBazel(
      intelliJ: RunningIntelliJFixture,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    val importSpec = intelliJ.config[BazelImportSpec]("bazel.import")
    openProjectWithBazel(intelliJ, importSpec, waitLogic)
  }

  def openProjectWithBazel(
      intelliJ: RunningIntelliJFixture,
      importSpec: BazelImportSpec,
      waitLogic: WaitLogic
  ): ProjectRef = {
    val robot = intelliJ.probe.withRobot.robot
    val projectView = prepareBazelProjectViewFile(importSpec, intelliJ.workspace)

    // delete previous project
    intelliJ.workspace.resolve(".ijwb").delete()

    // set bazel executable path
    BazelProbeDriver(intelliJ.probe).setupBazelExec(bazelPath(intelliJ.workspace))

    // start project import wizard
    intelliJ.probe.invokeActionAsync("Blaze.ImportProject2")

    // set workspace/repository root path
    robot
      .find(query.className("TextFieldWithHistory"))
      .setText(intelliJ.workspace.toRealPath().toString)
    robot.clickButton("Next")

    // set project view configuration file
    robot.clickRadioButton("Import project view file")
    robot.find(query.div("name" -> "projectview-file-path-field")).setText(projectView.toString)
    robot.clickButton("Next")

    // complete import wizard
    robot.clickButton("Finish")

    intelliJ.probe.await(waitLogic)

    projectView.delete()

    intelliJ.probe.listOpenProjects().head
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
}
