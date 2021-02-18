package org.virtuslab.ideprobe.pants

import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.Extensions._

trait PantsPluginExtraExtensions { this: IdeProbeFixture =>

  registerFixtureTransformer { fixture =>
    fixture.withAfterIntelliJInstall { (_, intelliJ) =>
      val plugins = intelliJ.paths.plugins
      plugins.resolve("Kotlin").delete()
      plugins.resolve("android").delete()
      intelliJ.paths.bin.resolve("printenv.py").makeExecutable()
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
