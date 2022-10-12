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

  private val referenceConfFile = new File("core/driver/sources/src/main/resources/reference.conf")
  private val ideProbeConfigTestFile =
    new File("core/driver/sources/src/test/scala/org/virtuslab/ideprobe/dependencies/IdeProbeConfigTest.scala")

  private val filesToUpdate = Seq(
    referenceConfFile,
    ideProbeConfigTestFile
  )

  private val releaseRegex = """20\d\d.\d.?\d?"""

  private val intellijReleaseFromReferenceConf = getValueFromReferenceConf("release = \"")
  private val intellijBuildNumberFromReferenceConf = getValueFromReferenceConf("build = \"")

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

  private lazy val newestStableBuildNumber = {
    val newestStableReleasePomLink = officialIntellijReleaseLinks.find(_.contains(newestStableRelease)).get
    def replaceIdeaICPomFileNameWithBuildTxt(ideaICpomFileName: String): String =
      ideaICpomFileName
        .replace("ideaIC-", "BUILD-")
        .replace(".pom", ".txt")
        .replace("ideaIC", "BUILD")

    val newestStableBuildNumberFileURL = new URL(replaceIdeaICPomFileNameWithBuildTxt(newestStableReleasePomLink))
    Source.fromURL(newestStableBuildNumberFileURL).mkString
  }

  if (newestStableRelease > intellijReleaseFromReferenceConf) filesToUpdate.foreach(bumpIntellijVersion)

  private def bumpIntellijVersion(oldFile: File): Unit = {
    val newFile = new File("/tmp/bump_version.txt")
    val w = new PrintWriter(newFile)
    Source
      .fromFile(oldFile)
      .getLines()
      .map { line =>
        if (line.contains(intellijReleaseFromReferenceConf))
          line.replace(intellijReleaseFromReferenceConf, newestStableRelease)
        else line
      }
      .map { line =>
        if (line.contains(intellijBuildNumberFromReferenceConf))
          line.replace(intellijBuildNumberFromReferenceConf, newestStableBuildNumber)
        else line
      }
      .foreach(x => w.println(x))
    w.close()
    Files.move(newFile.toPath, oldFile.toPath, REPLACE_EXISTING)
  }

  private def getValueFromReferenceConf(charsUntilValue: String): String = Source
    .fromFile(referenceConfFile)
    .getLines()
    .toList
    .find(line => line.contains(charsUntilValue) && isNotHoconComment(line, charsUntilValue))
    .get
    .trim
    .substring(charsUntilValue.length)
    .init // to drop the last double-quote character from the string value

  private def isNotHoconComment(line: String, keyChars: String): Boolean = {
    val commentStrings = Seq("//", "#")
    commentStrings.forall { commentString =>
      line.indexOf(commentString) == -1 || line.indexOf(keyChars) < line.indexOf(commentString)
    }
  }

}
