package org.virtuslab.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

object Screenshot {
  private val id = new AtomicInteger(0)

  private val probeDisplay = sys.env.get("DISPLAY")
  private val outputDirectory = for {
    outputDir <- sys.env.get("IDEPROBE_OUTPUT_DIR")
    testSuite <- sys.env.get("IDEPROBE_TEST_SUITE")
    testCase <- sys.env.get("IDEPROBE_TEST_CASE")
    path = Paths.get(outputDir).resolve(testSuite).resolve(testCase)
  } yield Files.createDirectories(path)

  def take(nameSuffix: String = ""): Unit = {
    for {
      display <- probeDisplay
      directory <- outputDirectory
      file = directory.resolve(s"${id.incrementAndGet()}$nameSuffix.png")
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
