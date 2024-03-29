package org.virtuslab.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

object Screenshot {
  private val id = new AtomicInteger(0)

  private val probeDisplay = sys.env.get("DISPLAY")
  private val testSuiteName = sys.env.getOrElse("IDEPROBE_TEST_SUITE", "")
  private val testCaseName = sys.env.getOrElse("IDEPROBE_TEST_CASE", "")
  private val screenshotsDir = sys.env("IDEPROBE_SCREENSHOTS_DIR")
  private val outputDirectory =
    Files.createDirectories(Paths.get(screenshotsDir).resolve(testSuiteName).resolve(testCaseName))

  def take(nameSuffix: String = ""): Unit = {
    for {
      display <- probeDisplay
      file = outputDirectory.resolve(s"${id.incrementAndGet()}$nameSuffix.png")
    } {
      val process = Runtime.getRuntime.exec(command(display, file))
      if (process.waitFor() != 0) {
        val stream = process.getInputStream
        while (stream.available() > 0) System.err.write(stream.read())
      }
    }
  }

  private def command(display: String, outputFile: Path): Array[String] = {
    Array("/bin/sh", "-c", s"xwd -display $display -root -silent | convert xwd:- png:${outputFile.toAbsolutePath}")
  }
}
