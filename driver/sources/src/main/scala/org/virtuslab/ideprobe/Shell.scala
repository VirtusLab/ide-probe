package org.virtuslab.ideprobe

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Path

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessHandler

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

object Shell {
  case class CommandResult(outSafe: String, err: String, exitCode: Int) {
    def out: String = {
      if (exitCode != 0) throw new RuntimeException(s"Command failed with exit code $exitCode") else outSafe
    }
  }

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
  }

  def async(command: String*): Future[CommandResult] = {
    async(in = null, command)
  }

  def async(in: Path, command: Seq[String]): Future[CommandResult] = {
    async(in, Map.empty[String, String], command)
  }

  def async(in: Path, env: Map[String, String], command: Seq[String]): Future[CommandResult] = {
    val location = if (in == null) "" else s" (in $in)"
    println(s"Executing command$location: ${command.mkString(" ")}")

    import com.zaxxer.nuprocess._
    val builder = new NuProcessBuilder(command: _*)
    builder.setCwd(in)
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

  def run(command: String*): CommandResult = {
    run(in = null, command: _*)
  }

  def run(in: Path, command: String*): CommandResult = {
    run(in, Map.empty[String, String], command: _*)
  }

  def run(in: Path, env: Map[String, String], command: String*): CommandResult = {
    Await.result(async(in, env, command), Duration.Inf)
  }
}
