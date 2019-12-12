package com.virtuslab

import com.virtuslab.ProbeHandlers.ProbeHandler
import com.virtuslab.handlers.Actions
import com.virtuslab.handlers.App
import com.virtuslab.handlers.BackgroundTasks
import com.virtuslab.handlers.Builds
import com.virtuslab.handlers.Freezes
import com.virtuslab.handlers.IdeMessages
import com.virtuslab.handlers.Modules
import com.virtuslab.handlers.Navigation
import com.virtuslab.handlers.Notifications
import com.virtuslab.handlers.PSI
import com.virtuslab.handlers.Plugins
import com.virtuslab.handlers.Projects
import com.virtuslab.handlers.RunConfigurations
import com.virtuslab.handlers.VFS
import com.virtuslab.ideprobe.protocol.Endpoints
import scala.concurrent.ExecutionContext

class BaseProbeHandlerContributor extends ProbeHandlerContributor {
  private implicit val ec: ExecutionContext = IdeProbeService.executionContext

  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(Endpoints.PID)(_ => App.pid)
      .on(Endpoints.Ping)(_ => ())
      .on(Endpoints.Plugins)(_ => Plugins.list)
      .on(Endpoints.Shutdown)(_ => App.shutdown())
      .on(Endpoints.Messages)(_ => IdeMessages.list)
      .on(Endpoints.Freezes)(_ => Freezes.list)
      .on(Endpoints.InvokeAction)(Actions.invoke)
      .on(Endpoints.InvokeActionAsync)(Actions.invokeAsync)
      .on(Endpoints.FileReferences)(PSI.references)
      .on(Endpoints.Find)(Navigation.find)
      .on(Endpoints.OpenProject)(Projects.open)
      .on(Endpoints.CloseProject)(Projects.close)
      .on(Endpoints.ProjectModel)(Projects.model)
      .on(Endpoints.ProjectSdk)(Projects.sdk)
      .on(Endpoints.ListOpenProjects)(_ => Projects.all)
      .on(Endpoints.ModuleSdk)(Modules.sdk)
      .on(Endpoints.AwaitIdle)(_ => BackgroundTasks.awaitNone())
      .on(Endpoints.Build)(Builds.build)
      .on(Endpoints.SyncFiles)(_ => VFS.syncAll())
      .on(Endpoints.AwaitNotification)(Notifications.await)
      .on(Endpoints.Run)(RunConfigurations.execute)
      .on(Endpoints.RunJUnit)(RunConfigurations.execute)
      .on(Endpoints.TakeScreenshot)(Screenshot.take)
  }
}
