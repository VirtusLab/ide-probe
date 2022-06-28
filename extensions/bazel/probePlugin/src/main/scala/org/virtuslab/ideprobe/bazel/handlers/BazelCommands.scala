package org.virtuslab.ideprobe.bazel.handlers

import scala.jdk.CollectionConverters._

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.model.primitives.TargetExpression
import com.google.idea.blaze.base.run.BlazeCommandRunConfigurationType
import com.google.idea.blaze.base.run.state.BlazeCommandRunConfigurationCommonState
import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl

import org.virtuslab.ideprobe.bazel.protocol.BazelCommandParams
import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.handlers.Projects
import org.virtuslab.ideprobe.handlers.RunConfigurations
import org.virtuslab.ideprobe.handlers.Tests
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.TestsRunResult

object BazelCommands extends IntelliJApi {
  def runTest(command: BazelCommandParams, ref: ProjectRef): TestsRunResult = {
    val project = Projects.resolve(ref)
    val factory = BlazeCommandRunConfigurationType.getInstance.getFactory
    val config = factory.createTemplateConfiguration(project)
    config.setTargets(ImmutableList.copyOf(command.targets.map(TargetExpression.fromString).toArray))
    val state = config.getHandlerStateIfType(classOf[BlazeCommandRunConfigurationCommonState])
    state.getCommandState.setCommand(BlazeCommandName.fromString(command.name))
    state.getExeFlagsState.setRawFlags(command.executableFlags.asJava)
    state.getBlazeFlagsState.setRawFlags(command.bazelFlags.asJava)
    config.setGeneratedName()

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = runManager.createConfiguration(config, factory)
    RunManager.getInstance(project).addConfiguration(settings)

    Tests.awaitTestResults(project, () => RunConfigurations.launch(project, settings))
  }
}
