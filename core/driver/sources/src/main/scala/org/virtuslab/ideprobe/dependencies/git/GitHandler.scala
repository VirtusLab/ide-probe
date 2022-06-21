package org.virtuslab.ideprobe.dependencies.git

import org.eclipse.jgit.api.{CloneCommand, CreateBranchCommand, Git}
import org.eclipse.jgit.lib.{Constants, ProgressMonitor, Ref}

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.util.Try
import org.virtuslab.ideprobe.Extensions._

object GitHandler {

  implicit class GitRepository(repository: String) {
    def clone(workspace: Path): Git = {
      val command: CloneCommand = new CloneCommand()
        .setURI(repository)
        .setDirectory(workspace.toFile)
        .setCloneAllBranches(true)
        .setMirror(false)
        .setBare(false)
        .setProgressMonitor(new CustomProgressMonitor(repository))
      val git =
        try {command.call() }
        catch {
            case e: Exception => throw new IllegalStateException(s"Could not clone git $repository: ${e.getMessage}")
          }
      git
    }
  }

  implicit class GitUriRepository(repository: URI) extends GitRepository(repository.toString)

  implicit class GitCheckout( git: Git) {
    def checkout(ref: String): Ref = {
      val checkoutBase = git.checkout()
      val checkoutCmd =
        if(git.getRepository.resolve(ref) != null)
          checkoutBase.setName(ref)
        else
          checkoutBase
            .setCreateBranch(true)
            .setName(ref)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(s"origin/$ref")
      val res =
        try{checkoutCmd.call()}
        catch {
          case e: Exception => throw new IllegalStateException(s"Could not checkout $ref in ${git.getRepository}: ${e.getMessage}")
        }
      res
    }
  }

  private class CustomProgressMonitor(repository: String) extends ProgressMonitor {

    private val totalWork: AtomicInteger = new AtomicInteger()
    private val completion: AtomicInteger = new AtomicInteger()
    private val task: AtomicReference[String] = new AtomicReference()

    @inline
     private def renderProgress(completed: Int): String = {
      val total = totalWork.get()
      val title = task.get()
      val inner =
        if(total == 0)
          ""
        else if (completed == total)
          s"$title 100% ($total/$total)\n"
        else {
          val percent = (100 * completed) / total
          s"$title $percent% ($completed/$total)"
        }
      s"\r$inner"
    }

    override def start(totalTasks: Int): Unit = {
      println(s"Cloning:$repository")
    }

    override def beginTask(title: String, totalWork: Int): Unit = {
      this.totalWork.set(totalWork)
      this.completion.set(0)
      this.task.set(title)
    }

    override def update(completed: Int): Unit = {
      val current = completion.getAndUpdate(_ + completed)
      print(renderProgress(current))
    }

    override def endTask(): Unit = {
      println("")
    }

    override def isCancelled: Boolean = false
  }
}
