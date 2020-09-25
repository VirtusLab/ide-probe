package org.virtuslab.ideprobe

import com.zaxxer.nuprocess.{NuAbstractProcessHandler, NuProcess, NuProcessBuilder, NuProcessHandler}
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.{Buffer, ByteBuffer}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object Shell extends BaseShell

case class CommandResult(outSafe: String, err: String, exitCode: Int) {
  def isSuccess: Boolean = exitCode == 0

  def out: String = {
    ok()
    outSafe
  }

  def ok(): Unit = {
    if (!isSuccess) throw new RuntimeException(s"Command failed with exit code $exitCode")
  }
}

class ShellInDirectory(in: Path, shell: BaseShell) {
  def async(command: Seq[String]): Future[CommandResult] = {
    shell.async(in, Map.empty[String, String], command)
  }

  def async(env: Map[String, String], command: Seq[String]): Future[CommandResult] = {
    shell.async(in, env, command)
  }

  def run(command: String*): CommandResult = {
    shell.run(in, command: _*)
  }

  def run(env: Map[String, String], command: String*): CommandResult = {
    shell.run(in, env, command: _*)
  }
}

class BaseShell {
  def in(workingDirectory: Path): ShellInDirectory = new ShellInDirectory(workingDirectory, this)

  class ProcessOutputLogger extends NuAbstractProcessHandler {
    private val outputChannel = Channels.newChannel(System.out)
    private val errorChannel = Channels.newChannel(System.err)

    override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
      outputChannel.write(buffer)
    }

    override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
      errorChannel.write(buffer)
    }
  }

  class ProcessOutputCollector extends NuAbstractProcessHandler {
    private val outputBuilder = new StringBuilder
    private val errorBuilder = new StringBuilder

    override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
      outputBuilder.append(StandardCharsets.UTF_8.decode(buffer))
    }

    override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
      errorBuilder.append(StandardCharsets.UTF_8.decode(buffer))
    }

    def output: String = outputBuilder.toString()
    def error: String = errorBuilder.toString()
  }

  class CompositeOutputHandler(handlers: Seq[NuProcessHandler]) extends NuAbstractProcessHandler {
    override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
      handlers.foreach { handler =>
        buffer.asInstanceOf[Buffer].rewind()
        handler.onStdout(buffer, closed)
      }
    }
    override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
      handlers.foreach { handler =>
        buffer.asInstanceOf[Buffer].rewind()
        handler.onStderr(buffer, closed)
      }
    }

    override def onPreStart(nuProcess: NuProcess): Unit = handlers.foreach(_.onPreStart(nuProcess))

    override def onStart(nuProcess: NuProcess): Unit = handlers.foreach(_.onStart(nuProcess))

    override def onExit(statusCode: Int): Unit = handlers.foreach(_.onExit(statusCode))

    override def onStdinReady(buffer: ByteBuffer): Boolean = handlers.exists(_.onStdinReady(buffer))
  }

  def async(in: Path, env: Map[String, String], command: Seq[String]): Future[CommandResult] = {
    val location = if (in == null) "" else s" (in $in)"
    println(s"Executing command$location:\n$$ ${command.mkString(" ")}")

    import com.zaxxer.nuprocess._
    val builder = new NuProcessBuilder(command: _*)
    builder.setCwd(in)
    customizeBuilder(builder)
    env.foreach(e => builder.environment().put(e._1, e._2))
    val finished = Promise[CommandResult]()
    val outputCollector = new ProcessOutputCollector
    val outputForwarder = new ProcessOutputLogger
    builder.setProcessListener(new CompositeOutputHandler(Seq(outputCollector, outputForwarder)) {
      override def onExit(statusCode: Int): Unit = {
        finished.success(CommandResult(outputCollector.output.trim, outputCollector.error.trim, statusCode))
      }
    })

    builder.start()
    finished.future
  }

  def async(command: String*): Future[CommandResult] = {
    async(in = null, command)
  }

  def async(in: Path, command: Seq[String]): Future[CommandResult] = {
    async(in, Map.empty[String, String], command)
  }

  def run(command: String*): CommandResult = {
    run(in = null, command: _*)
  }

  def run(in: Path, command: String*): CommandResult = {
    run(in, Map.empty[String, String], command: _*)
  }

  def run(in: Path, env: Map[String, String], command: String*): CommandResult = {
    Await.result(async(in, env, command), Duration.Inf)
  }

  protected def customizeBuilder(builder: NuProcessBuilder): Unit = ()
}
