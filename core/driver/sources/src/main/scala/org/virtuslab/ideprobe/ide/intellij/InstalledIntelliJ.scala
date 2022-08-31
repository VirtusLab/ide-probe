package org.virtuslab.ideprobe.ide.intellij

import java.io.File
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import scala.concurrent.ExecutionContext
import scala.concurrent.blocking

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.DriverConfig
import org.virtuslab.ideprobe.jsonrpc.JsonRpcConnection

sealed abstract class InstalledIntelliJ(root: Path, probePaths: IdeProbePaths, config: DriverConfig) {
  def cleanup(): Unit

  def paths: IntelliJPaths

  protected final val ideaPropertiesContent: String =
    s"""|idea.config.path=${paths.config}
        |idea.system.path=${paths.system}
        |idea.plugins.path=${paths.plugins}
        |idea.log.path=${paths.logs}
        |java.util.prefs.userRoot=${paths.userPrefs}
        |""".stripMargin

  protected val implementationSpecificVmOptions: Seq[String] = Seq.empty

  protected lazy val vmoptions: Path = {
    val baseVMOptions = Seq(
      s"-Djava.awt.headless=${config.headless}",
      "-Djb.privacy.policy.text=<!--999.999-->",
      "-Djb.consents.confirmation.enabled=false"
    )

    val vmOptions = implementationSpecificVmOptions ++ baseVMOptions ++ DebugMode.vmOption ++ config.vmOptions
    val content = vmOptions.mkString("\n")

    root.resolve("bin").resolve("ideprobe.vmoptions").write(content)
  }

  protected def ideaProperties: Path

  def startIn(workingDir: Path, probeConfig: Config)(implicit ec: ExecutionContext): RunningIde = {
    val server = new ServerSocket(0)

    val launcher = startProcess(workingDir, server)
    try {
      server.setSoTimeout(config.launch.timeout.toMillis.toInt)
      val socket = blocking(server.accept()) // will be closed along with the connection by ProbeDriver
      val connection = JsonRpcConnection.from(socket)

      val driver = ProbeDriver.start(connection, probeConfig)
      driver.setConfig(probeConfig)
      driver.addTrustedPath(probePaths.trusted)
      new RunningIde(launcher, driver.pid(), driver)
    } catch {
      case cause: Exception =>
        launcher.destroy(true)
        throw cause
    } finally {
      // we only need the server to establish the initial connection
      server.close()
    }
  }

  private val executable: Path = {
    val content = {
      val macOsLauncher = paths.root.resolve("MacOS").resolve("idea")
      val launcher =
        if (OS.Current == OS.Mac && macOsLauncher.toFile.exists())
          macOsLauncher
        else
          paths.bin.resolve("idea.sh")

      launcher.makeExecutable()

      val command =
        if (config.headless) s"$launcher headless"
        else {
          import config.xvfb.screen._
          Display.Mode match {
            case Display.Native => s"$launcher"
            case Display.Xvfb =>
              s"""xvfb-run --server-num=${Display.XvfbDisplayId} --server-args="-screen 0 ${width}x${height}x${depth}" $launcher"""
          }
        }

      s"""|#!/bin/sh
          |$command "$$@"
          |""".stripMargin
    }

    paths.bin
      .resolve("idea")
      .write(content)
      .makeExecutable()
  }

  private def startProcess(workingDir: Path, server: ServerSocket) = {
    val command = config.launch.command.toList match {
      case Nil =>
        List(executable.toString)
      case "idea" :: tail =>
        executable.toString :: tail
      case nonEmpty =>
        nonEmpty
    }

    val environment = {
      val PATH = List(paths.bin, System.getenv("PATH"))
        .mkString(File.pathSeparator)

      val testCaseEnv: Map[String, String] = TestCase.current match {
        case None =>
          Map.empty
        case Some(testCase) =>
          Map(
            "IDEPROBE_TEST_SUITE" -> testCase.suite,
            "IDEPROBE_TEST_CASE" -> testCase.name
          )
      }

      val overrideDisplay =
        if (Display.Mode == Display.Xvfb) Map("DISPLAY" -> s":${Display.XvfbDisplayId}")
        else Map.empty
      testCaseEnv ++ Map(
        "IDEA_VM_OPTIONS" -> vmoptions.toString,
        "IDEA_PROPERTIES" -> ideaProperties.toString,
        "IDEPROBE_DRIVER_PORT" -> server.getLocalPort.toString,
        "IDEPROBE_SCREENSHOTS_DIR" -> probePaths.screenshots.toString,
        "PATH" -> PATH
      ) ++ overrideDisplay ++ config.env
    }

    val builder = new NuProcessBuilder(command.asJava)

    builder.setCwd(workingDir)
    builder.setProcessListener(new Shell.ProcessOutputLogger)
    builder.environment().putAll(environment.asJava)

    println(s"Starting process ${command.mkString(" ")} in $workingDir")

    val processHandler = new NuAbstractProcessHandler {
      override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed || buffer.remaining > 0)
          printBuffer(buffer, "intellij-stderr")
      }

      override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed || buffer.remaining > 0)
          printBuffer(buffer, "intellij-stdout")
      }

      private def printBuffer(buffer: ByteBuffer, tag: String): Unit = {
        val bytes = new Array[Byte](buffer.remaining)
        buffer.get(bytes)
        val output = new String(bytes)
        val lines = output.linesIterator
        lines.foreach(line => println(s"[$tag] $line"))
      }
    }
    builder.setProcessListener(processHandler)

    builder.start()
  }
}

final class LocalIntelliJ(
    val root: Path,
    probePaths: IdeProbePaths,
    config: DriverConfig,
    val paths: IntelliJPaths,
    pluginsBackup: Path
) extends InstalledIntelliJ(root, probePaths, config) {
  override protected val ideaProperties: Path = root.resolve("bin").resolve("idea.properties")

  private val ideaPropertiesBackup: Option[Path] = if (ideaProperties.isFile) {
    val tempPath = Files.createTempFile(root, "idea.properties", ".backup")
    Some(ideaProperties.copyTo(tempPath))
  } else None

  Files.write(ideaProperties, ideaPropertiesContent.getBytes(), StandardOpenOption.APPEND)

  override protected val implementationSpecificVmOptions: Seq[String] = {
    val idea64VmOptions = root.resolve("bin").resolve("idea64.vmoptions")
    if (idea64VmOptions.isFile) {
      idea64VmOptions.lines()
    } else {
      Seq.empty
    }
  }

  override def cleanup(): Unit = {
    cleanupIdeaProperties()
    val pluginsDir = root.resolve("plugins")
    pluginsDir.delete()
    pluginsBackup.moveTo(pluginsDir)
    vmoptions.delete()
  }

  private def cleanupIdeaProperties(): Unit =
    ideaPropertiesBackup.foreach(_.moveTo(ideaProperties, replace = true))
}

final class DownloadedIntelliJ(
    root: Path,
    probePaths: IdeProbePaths,
    val paths: IntelliJPaths,
    config: DriverConfig
) extends InstalledIntelliJ(root, probePaths, config) {
  override val ideaProperties: Path =
    root.resolve("bin").resolve("idea.properties").write(ideaPropertiesContent)

  override def cleanup(): Unit = {
    probePaths.logExport.foreach { path =>
      paths.logs.copyDir(path.resolve(getPathWithVersionNumber(root).getFileName).resolve("logs"))
    }
    root.delete()
  }

  /*
  Method below helps receive the path of the intellij instance directory. This path contains intellij version number
  in its string representation. It might be useful for scenarios, where tests run on multiple intellij versions.
  In such cases users will have logs grouped by intellij versions. The `if` expression is needed as
  for now the `intellijRootPath` might have different structure, like in examples below:
  a) /.../intellij-instance-2022.2.1--T3ySdgShSvyr87HNoJq-oQ/    -> for Linux-based OS
  b) /.../intellij-instance-2022.2.1--T3ySdgShSvyr87HNoJq-oQ/Contents    -> for macOs
   */
  private def getPathWithVersionNumber(intellijRootPath: Path): Path =
    if (intellijRootPath.name == "Contents") intellijRootPath.getParent else intellijRootPath

}
