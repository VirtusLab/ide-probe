package org.virtuslab.ideprobe.pants

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbeFixture

trait PantsPluginExtraExtensions { this: IdeProbeFixture =>

  registerFixtureTransformer { fixture =>
    fixture.withAfterIntelliJInstall { (_, intelliJ) =>
      val plugins = intelliJ.paths.bundledPlugins
      plugins.resolve("Kotlin").delete()
      plugins.resolve("android").delete()
    }
  }

  registerFixtureTransformer { fixture =>
    fixture.withAfterWorkspaceSetup { (_, workspace) =>
      BspWorkspaceMonitor.register(workspace)
    }
  }

  registerFixtureTransformer { fixture =>
    fixture.withAfterWorkspaceSetup { (fixture, workspace) =>
      FastpassSetup.overrideFastpassVersion(fixture, workspace)
    }
  }

}
