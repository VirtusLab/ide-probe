//> using scala "2.13"
//> using lib "org.virtuslab.ideprobe::driver:0.31.0"
//> using lib "org.virtuslab.ideprobe::benchmarks:0.31.0"
//> using lib "com.lihaoyi::os-lib:0.8.1"

import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.wait.WaitLogicConfigFormat
import org.virtuslab.ideprobe.wait.WaitLogicFactory


object main extends IdeProbeFixture with WaitLogicFactory {

  def prepareIdeGeneralXml(installDir: os.Path): Unit = {
    val ideGeneralXml =
      """<application>
        |  <component name="Registry">
        |    <entry key="shared.indexes.download.auto.consent" value="true" />
        |  </component>
        |</application>""".stripMargin
    os.makeDir.all(installDir / "options")
    os.write.over(installDir / "options" / "ide.general.xml", ideGeneralXml)
  }

  val config = s"""
                  |probe {
                  |  driver {
                  |    vmOptions = [ "-Dgit.process.ignored=false", "-Xms2g" ]
                  |    headless = true
                  |}
                  |
                  |  intellij {
                  |    path = "/ide-desktop/backend/"
                  |    plugins = []
                  |  }
                  |
                  |  workspace {
                  |    existing = "/workspace/ide-probe"
                  | }
                  |}
                  |
  """.stripMargin

  def main(args0: Array[String]): Unit = {
    fixtureFromConfig(Config.fromString(config))
      .withAfterIntelliJInstall{ case (fixture, ij) => prepareIdeGeneralXml(os.Path("/ide-desktop/backend/"))}
      .run { ij =>ij.probe.openProject(ij.workspace, emptyNamedBackgroundTasks())}
  }

}