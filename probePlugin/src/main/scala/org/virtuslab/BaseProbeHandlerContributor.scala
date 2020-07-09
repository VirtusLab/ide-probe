package org.virtuslab

import org.virtuslab.ProbeHandlers.ProbeHandler
import org.virtuslab.handlers._
import org.virtuslab.ideprobe.protocol.Endpoints
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
      .on(Endpoints.VcsRoots)(VCS.roots)
  }
}
