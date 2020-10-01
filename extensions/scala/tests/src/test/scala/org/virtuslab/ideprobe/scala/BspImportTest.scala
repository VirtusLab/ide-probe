package org.virtuslab.ideprobe.scala

import java.nio.file.{Files, Paths, StandardOpenOption}
import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Config
import scala.concurrent.Future
import scala.util.Try

final class BspImportTest extends ScalaPluginTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    fixtureFromConfig(config).run { intelliJ =>
      val robot = intelliJ.probe.withRobot.robot
      val projectRef = Future {
        println("!!!!!!!!!!! Openning")
        intelliJ.probe.importBspProject(intelliJ.workspace.resolve("root"))
        println("!!!!!!!!!!! Opened")
      }
      while (true) {
        println("!!!!!!!!!!! querying")
        robot.findOpt(query.className("MultipleBuildsPanel")).foreach { buildPanel =>
          val tree = buildPanel.find(query.className("Tree"))
          val treeTexts = tree.fullTexts
          val hasErrors = treeTexts.contains("failed")
          val message = buildPanel
            .find(query.div("accessiblename" -> "Editor", "class" -> "EditorComponentImpl"))
            .fullText
          println(s"!!!!!!! $message")
          val path = Paths.get("/tmp/ideprobe/output/out.txt")
          if (Files.notExists(path)) Files.createFile(path)
          Files.writeString(path, message + "\n", StandardOpenOption.APPEND)
        }
        Thread.sleep(200)
      }
    }
  }

}
