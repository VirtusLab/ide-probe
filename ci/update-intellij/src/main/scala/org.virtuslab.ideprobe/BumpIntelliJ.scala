package org.virtuslab.ideprobe

import java.io.File
import java.io.PrintWriter
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import scala.io.Source
import scala.jdk.CollectionConverters._

import org.jsoup.Jsoup

object BumpIntelliJ extends App {
  private val fileName = "intellijVersion.properties"
  private val releaseRegex = """20\d\d.\d.?\d?"""

  val filesToUpdate = Seq(
    new File("ci/update-intellij/src/main/resources/intellijVersion.properties"),
    new File("core/driver/sources/src/main/resources/reference.conf"),
    new File("core/driver/sources/src/test/scala/org/virtuslab/ideprobe/dependencies/IdeProbeConfigTest.scala")
  )

  private val intellijReleaseFromCode = getValueFromIntellijVersionFile("latestRelease")
  private val intellijBuildNumberFromCode = getValueFromIntellijVersionFile("connectedBuildNumber")

  private val officialIntellijReleaseLinks: List[String] =
    Jsoup
      .connect("https://www.jetbrains.com/intellij-repository/releases/")
      .get()
      .select(
        "a[href^=https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/][href$=.pom]"
      )
      .eachAttr("href")
      .asScala
      .toList

  private val officialIntellijReleases =
    officialIntellijReleaseLinks
      .map(link => link.substring(link.indexOf("ideaIC-"), link.indexOf(".pom")).replace("ideaIC-", ""))
      .filter(_.matches(releaseRegex))

  private val newestStableRelease = officialIntellijReleases.max

  private lazy val newestStableReleasePomLink = officialIntellijReleaseLinks.find(_.contains(newestStableRelease)).get
  private lazy val newestStableBuildNumber = {
    def replaceIdeaICPomFileNameWithBuildTxt(ideaICpomFileName: String): String =
      ideaICpomFileName
        .replace("ideaIC-", "BUILD-")
        .replace(".pom", ".txt")
        .replace("ideaIC", "BUILD")

    val newestStableBuildNumberFileURL = new URL(replaceIdeaICPomFileNameWithBuildTxt(newestStableReleasePomLink))
    Source.fromURL(newestStableBuildNumberFileURL).mkString
  }

  if (newestStableRelease > intellijReleaseFromCode) filesToUpdate.foreach(bumpIntellijVersion)

  private def bumpIntellijVersion(oldFile: File): Unit = {
    val newFile = new File("/tmp/bump_version.txt")
    val w = new PrintWriter(newFile)
    Source
      .fromFile(oldFile)
      .getLines()
      .map { line =>
        if (line.contains(intellijReleaseFromCode)) line.replace(intellijReleaseFromCode, newestStableRelease) else line
      }
      .map { line =>
        if (line.contains(intellijBuildNumberFromCode))
          line.replace(intellijBuildNumberFromCode, newestStableBuildNumber)
        else line
      }
      .foreach(x => w.println(x))
    w.close()
    Files.move(newFile.toPath, oldFile.toPath, REPLACE_EXISTING)
  }

  private def getValueFromIntellijVersionFile(key: String): String = Source
    .fromResource(fileName)
    .getLines()
    .toList
    .find(_.contains(key))
    .get
    .substring(key.length + 1) // to extract VALUE from "$key=VALUE" line

}
