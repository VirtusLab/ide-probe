package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

import org.virtuslab.ideprobe.protocol.ProjectRef

import scala.concurrent.duration.DurationInt

object WaitLogicFactory {
  val DefaultCheckFrequency: FiniteDuration = 5.seconds
  val DefaultEnsurePeriod: FiniteDuration = 2.seconds
  val DefaultEnsureFrequency: FiniteDuration = 50.millis
  val DefaultAtMost: FiniteDuration = 15.minutes
}

trait WaitLogicFactory {
  import WaitLogicFactory._

  val Default = new ConfigWaiting("probe.waitLogic.default", emptyNamedBackgroundTasks())

  val OnStartup = new ConfigWaiting(
    "probe.waitLogic.onStartup",
    emptyNamedBackgroundTasks(
      basicCheckFrequency = 0.seconds,
      ensurePeriod = 500.millis,
      atMost = 1.minute
    )
  )

  def constant(duration: FiniteDuration): ConstantWaiting = {
    new ConstantWaiting(duration)
  }

  def none: NoWaiting.type = NoWaiting

  def chain(waitLogics: WaitLogic*): ChainedWaiting = {
    new ChainedWaiting(waitLogics)
  }

  def basic(
      checkFrequency: FiniteDuration = DefaultCheckFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): BasicDSL = {
    new BasicDSL(checkFrequency, atMost)
  }

  def forUnstableCondition(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): UnstableConditionDSL = {
    new UnstableConditionDSL(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost)
  }

  def emptyBackgroundTasks(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): UnstableConditionWaiting = {
    backgroundTasksWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost
    ) { currentTasks =>
      if (currentTasks.nonEmpty) {
        WaitDecision.KeepWaiting(s"Waiting for ${currentTasks.size} background task(s)...")
      } else {
        WaitDecision.Done
      }
    }
  }

  def emptyNamedBackgroundTasks(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): UnstableConditionWaiting = {
    backgroundTasksWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost
    ) { currentTasks =>
      val ignorePatterns = Seq(
        "<unknown>".r,
        """ProgressIndicator(?:Ex)? \d+: .*""".r,
        """com\.intellij\.openapi\.progress\.EmptyProgressIndicator@.{8}""".r
      )
      def isNamedTask(task: String): Boolean =
        !ignorePatterns.exists(pattern => pattern.unapplySeq(task).isDefined)
      val namedCurrentTasks = currentTasks.filter(isNamedTask)
      if (namedCurrentTasks.nonEmpty) {
        WaitDecision.KeepWaiting(
          s"Waiting for ${namedCurrentTasks.size} background task(s)... ${format(namedCurrentTasks)}"
        )
      } else {
        WaitDecision.Done
      }
    }
  }

  def backgroundTaskExists(
      checkFrequency: FiniteDuration = 1.second,
      atMost: FiniteDuration = DefaultAtMost
  )(task: String, tasks: String*): BasicWaiting = {
    val expectedTasks = task +: tasks
    backgroundTasksBasic(checkFrequency, atMost) { currentTasks =>
      val taskExists = currentTasks.exists { currentTask =>
        expectedTasks.exists(expectedTask => currentTask.contains(expectedTask))
      }
      if (!taskExists) {
        WaitDecision.KeepWaiting(s"Waiting for ${format(expectedTasks)} to appear")
      } else {
        WaitDecision.Done
      }
    }
  }

  def backgroundTaskExists(task: String, tasks: String*): BasicWaiting = {
    backgroundTaskExists()(task, tasks: _*)
  }

  def backgroundTaskNotExists(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  )(task: String, tasks: String*): UnstableConditionWaiting = {
    val expectedTasks = task +: tasks
    backgroundTasksWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost
    ) { currentTasks =>
      val matchingTasks = currentTasks.filter { currentTask =>
        expectedTasks.exists(expectedTask => currentTask.contains(expectedTask))
      }
      if (matchingTasks.nonEmpty) {
        WaitDecision.KeepWaiting(s"Waiting for ${format(matchingTasks)} to complete...")
      } else {
        WaitDecision.Done
      }
    }
  }

  def backgroundTaskNotExists(task: String, tasks: String*): UnstableConditionWaiting = {
    backgroundTaskNotExists()(task, tasks: _*)
  }

  def backgroundTaskCompletes(
      task: String,
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      maxWaitBeforeTask: FiniteDuration = DefaultAtMost,
      maxTaskDuration: FiniteDuration = DefaultAtMost
  ): ChainedWaiting = {
    val beforeTask = backgroundTaskExists(basicCheckFrequency, maxWaitBeforeTask)(task)
    val duringTask =
      backgroundTaskNotExists(basicCheckFrequency, ensurePeriod, ensureFrequency, maxTaskDuration)(
        task
      )
    WaitLogic.chain(beforeTask, duringTask)
  }

  def projectByName(
      expectedName: String,
      checkFrequency: FiniteDuration = DefaultCheckFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): BasicWaiting = {
    val expectedRef = ProjectRef(expectedName)
    basic(checkFrequency, atMost) { driver =>
      val projects = driver.listOpenProjects()
      if (!projects.contains(expectedRef)) {
        val suffix = if (projects.nonEmpty) s" Currently open projects: ${format(projects)}" else ""
        val message = s"Waiting for opening project '$expectedName'...$suffix"
        WaitDecision.KeepWaiting(message)
      } else {
        WaitDecision.Done
      }
    }
  }

  def projectByModules(
      expectedModules: Set[String],
      checkFrequency: FiniteDuration = DefaultCheckFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ): BasicWaiting = {
    basic(checkFrequency, atMost) { driver =>
      val modules = driver.projectModel().modules.map(_.name).toSet
      if (!expectedModules.subsetOf(modules)) {
        val message = {
          s"Failed to open project with modules ${format(expectedModules)}, " +
            s"loaded modules are: ${format(modules)}"
        }
        WaitDecision.KeepWaiting(message)
      } else {
        WaitDecision.Done
      }
    }
  }

  private def backgroundTasksBasic(checkFrequency: FiniteDuration, atMost: FiniteDuration)(
      waitCondition: Seq[String] => WaitDecision
  ) = {
    basic(checkFrequency, atMost) { driver =>
      val tasks = driver.backgroundTasks()
      waitCondition(tasks)
    }
  }

  private def backgroundTasksWithEnsurePeriod(
      basicCheckFrequency: FiniteDuration,
      ensurePeriod: FiniteDuration,
      ensureFrequency: FiniteDuration,
      atMost: FiniteDuration
  )(
      waitCondition: Seq[String] => WaitDecision
  ) = {
    forUnstableCondition(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost) { driver =>
      val tasks = driver.backgroundTasks()
      waitCondition(tasks)
    }
  }

  private def format(iter: Iterable[Any]): String = iter.mkString("[", ", ", "]")
}
