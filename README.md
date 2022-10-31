By using this library, you implicitly accept the terms of
the [JetBrains Privacy Policy](https://www.jetbrains.com/legal/docs/privacy/privacy.html).

# Description

ide-probe is a framework for testing plugins for IntelliJ-based IDEs. It can be used both locally and in the CI
pipeline.

The framework itself comprises three components:

- driver - responsible for controlling the workspace and installation, startup and communication with the IDE
- probePlugin - a server that runs inside the IDE and executes commands and queries
- api - a module that contains the definitions of endpoints and their protocol

# Motivation

Sometimes, unit tests cannot be used to reproduce a failure or test some specific feature reliably. This happens because
the testing environment used by IDEs most often differs from the actual, production-like environment experienced by the
user. Be it a disabled UI, forced single-threaded execution, or just a different control flow, the unit tests are just
not the right tool to use sometimes.

Using ide-probe fixes those problems at the non-avoidable cost of longer execution time when compared to unit tests.
With it, not only a proper environment is used, but one can also guard against new classes of errors, like:

- UI freezes
- background plugin errors
- unexpected behavior after starting or restarting IDE sessions
- invalid interactions between multiple IDE sessions running in parallel

# Getting Started

## Adding ide-probe to the project

To include ide-probe in your sbt project add following lines:

```scala
libraryDependencies += "org.virtuslab.ideprobe" %% "driver" % "0.24.0"
```

To use snapshots versions, add another repository:

```scala
resolvers.in(ThisBuild) += Resolver.sonatypeRepo("snapshots")
```

To use remote robot, add a resolver:

```scala
ThisBuild / resolvers += MavenRepository(
  "jetbrains-3rd",
  "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies"
)
```

For gradle, use:

```groovy
repositories {
    maven { url 'https://packages.jetbrains.team/maven/p/ij/intellij-dependencies' } // for robot extension
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' } // for snapshots only
}

dependencies {
    implementation group: 'org.virtuslab.ideprobe', name: 'driver_2.13', version: '0.24.0'
}
```

## Base test class setup

The default way to use ide-probe is to inherit from `IdeProbeFixture`. To use it, depend on `driver`
module (`"org.virtuslab.ideprobe" %% "driver" % version`). This trait provides `fixtureFromConfig` method. It is a
preferred way to create the `IntelliJFixture` which is used to run IntelliJ inside a workspace and interact with it.
Additionally, it allows adding fixture transformers. Fixture transformers are a convenient method of adding some common
behavior or configuration to a base trait for the tests. It is also the same mechanism that is used by extensions to add
their probe plugin and custom behavior.

It is possible to just use `IntelliJFixture.fromConfig` directly, but in such case, you need to remember to
call `.enableExtensions` method, or extensions will not be applied.

To write a test with JUnit 4 it is convenient to depend on `junit-driver` module, and extend from `IdeProbeTestSuite` in
the test class. It is the `IdeProbeFixture` with `@RunWith` annotation for convenience.

It is advised to prepare a base trait that extends from `IdeProbeFixture`, all required extensions and that contains
common fixture transformers to avoid repetition.

## Configuration

ide-probe uses [HOCON](https://github.com/lightbend/config) format for configuration. It is also possible to set up
everything in code, but config files might usually be more convenient. Additionally, using config files allows to easily
share configuration and create multiple variants of it for example for running the same test on different project.

Configuration can either be:

A) loaded from a file in classpath

```scala
fixtureFromConfig(Config.fromClasspath("path/example.conf"))
```

B) loaded from a file in the file system

```scala
fixtureFromConfig(Config.fromFile(Paths.get("/Users/user/Desktop/example.conf")))
```

C) provided as a string

```scala
fixtureFromConfig(Config.fromString("""probe { workspace.path = /foo/bar } """))
```

D) provided as a map

```scala
fixtureFromConfig(Config.fromMap(Map("probe.workspace.path" -> "/foo/bar")))
```

Usually it would be just used to programmatically override already loaded config, using helpful `withConfig` method on
IntelliJFixture.

```scala
fixtureFromConfig().withConfig("some.extra.config.override" -> "value", "other.config" -> "value2")
```

E) specified programmatically

```scala
IntelliJFixture(workspaceProvider = WorkspaceTemplate.fromFile(path))
  .withVersion(IntelliJVersion.release("2020.3", "202.8194.7"))
  .withPlugin(Plugin("org.intellij.scala", "2020.2.7"))
  .enableExtensions
```

1. [Driver](docs/driver.md)
2. [Resolvers](docs/custom-resolvers.md)
3. [Workspace](docs/workspace.md)
4. [Display](docs/display.md)
5. [Debugging](core/driver/sources/src/main/resources/reference.conf) (search for `probe.driver.debug` config)

## Workflow
Workflow can only be defined programmatically, since it comprises a sequence of intertwined:

1. probe interactions
2. workspace manipulation
3. custom verification logic

Below example shows a test that creates a new sbt file, thus creating a sbt project,
imports it, and check if the name is correct after the import.

It is composed of 2 files, the `ExampleTest.scala` and `example.conf`.

```scala
import org.virtuslab.ideprobe.Extensions._ // for `write` extension method on Path
import org.virtuslab.ideprobe.IdeProbeFixture
import org.junit.jupiter.api.Test

class ExampleTest extends IdeProbeFixture {
  private val fixture = fixtureFromConfig("example.conf")

  @Test def test() = {
    fixture.run { intelliJ =>
      val buildSbt = intelliJ.workspace.resolve("build.sbt")
      buildSbt.write(""" name := "example" """)

      intelliJ.probe.openProject(intelliJ.workspace)
      val projectModel = intelliJ.probe.projectModel()

      assert(projectModel.name == "example")
    }
  }

}
``` 

The contents of `example.conf` file located in resources:
```
probe {
  driver.vmOptions = [ "-Dgit.process.ignored=false", "-Xms2g" ]

  intellij {
    version {
      release = "2021.1.1"
      build = "211.7142.45"
    }
    plugins = [
      { id = "org.intellij.scala", version = "2021.1.18" }
    ]
  }
}
```

# Endpoints

To see the list of probe endpoints, see
[Commands](docs/endpoints/commands.md) or [Queries](docs/endpoints/queries.md). An always up to date list of queries is
available
in [Endpoints.scala](https://github.com/VirtusLab/ide-probe/tree/master/core/api/src/main/scala/org/virtuslab/ideprobe/protocol/Endpoints.scala)

Note that any communication with the probe is synchronous.

## Screenshots

The default folder for saving photos is `/tmp/ide-probe/screenshots`. Inside this directory
screenshots are organised into `test-suite/test-case` subfolders if these values are detectable by the plugin.
Otherwise, the photos are saved directly into `/tmp/ide-probe/screenshots`. 
The only way to override this default directory is by configuring `probe.paths.screenshots ` in the fixture, either through the `.conf` file or in code.
Overriding with `.conf` file looks like: 
```
probe {
   // ...
   paths.screenshots = ${?MY_IDEPROBE_SCREENSHOTS_DIR}
}`
```
Thanks to HOCON feature, it is possible to populate any config value from environment variable. Additionally, it is achievable to make this value optional
with question mark used before the name of the environment variable - if `MY_IDEPROBE_SCREENSHOTS_DIR`  does not exist at runtime
`probe.paths.screenshots` will use default value without any error.
For non `.conf` scenario, user would call: 
```
fixture.withPaths(IdeProbePaths(/*construct the instance passing, among others, a screenshots path that is most suitable for you*/))
```
Screenshots feature is only available with Xvfb display mode. They are taken on every AWT event, during probe shutdown (with _on-exit_ in screenshot name) and when explicitly requested via `probe.screenshot()`.
# Extensions

Extensions exist to implement custom actions specific to a plugin. For example
to create a ScalaTest run configuration, specific to Scala plugin, 
the Scala plugin extension is required. Similarly, to run Pants build, which is 
a custom action, it is best to use Pants extension that has it implemented.
You can create your own extension for your plugin if there is anything not covered
in existing endpoints.

To use an extension, add a dependency to your build and extend from appropriate trait.

## Pants, Bazel and Scala

For dependencies use:

```scala
libraryDependencies += "org.virtuslab.ideprobe" %% "EXTENSION-probe-driver" % ideProbeVersion

// example
libraryDependencies += "org.virtuslab.ideprobe" %% "scala-probe-driver" % "0.24.0"
```

Additionally extend from `org.virtuslab.ideprobe.EXTENSION.EXTENSIONPluginExtension`, 
for example `org.virtuslab.ideprobe.scala.ScalaPluginExtension` 

In the code example below, thanks to adding the `ScalaPluginExtension`, a new method
`setSbtProjectSettings` is added through implicit conversion.

```scala
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.protocol.Setting
import org.virtuslab.ideprobe.scala.ScalaPluginExtension
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest
import org.junit.jupiter.api.Test

class ExtensionExampleTest extends IdeProbeFixture with ScalaPluginExtension {
   private val fixture = fixtureFromConfig("example.conf")

   @Test def test() = {
      fixture.run { intelliJ =>
         val buildSbt = intelliJ.workspace.resolve("build.sbt")
         buildSbt.write(""" name := "example" """)

         val settings = SbtProjectSettingsChangeRequest(useSbtShellForBuild = Setting.Changed(true))
         intelliJ.probe.setSbtProjectSettings(settings)
         // or explicitly: ScalaProbeDriver(intelliJ.probe).setSbtProjectSettings(settings) 

         intelliJ.probe.openProject(intelliJ.workspace)
         val projectModel = intelliJ.probe.projectModel()
         assert(projectModel.name == "example")
      }
   }

}
``` 

## Robot

The robot is a slightly different extension. It integrates [Jetbrains Remote-Robot](https://github.com/JetBrains/intellij-ui-test-robot)
with ide-probe. This library can interact with UI of IntelliJ, click on specific components,
but also read text from them. Additionally, it allows invoking custom code inside IntelliJ's
JVM through JavaScript engine.

This extension automatically installs the plugin Remote-Robot plugin inside IntelliJ and establishes
connection between IntelliJ and tests, giving access to an instance of `RemoteRobot`.

To add it to your project, use:
```scala
libraryDependencies += "org.virtuslab.ideprobe" %% "robot-driver" % ideProbeVersion
```
and make sure you have the necessary resolver, see [Adding ide-probe to the project](#adding-ide-probe-to-the-project) 
section.

The test class below extends from `RobotPluginExtension` which adds new `withRobot` method on `intelliJ.probe`.
This method is a shorthand for creating the robot driver. Alternatively it can be created using
`RobotProbeDriver(intelliJ.probe)` call.

The `RobotProbeDriver` has a field called `robot` which is an instance of `RemoteRobot` that can be used
as described in JetBrains documentation. ide-probe also adds a simple DSL on top of existing one for simpler
use with Scala. You can check example usage in [RobotProbeDriver.scala](https://github.com/VirtusLab/ide-probe/blob/master/extensions/robot/driver/src/main/scala/org/virtuslab/ideprobe/robot/RobotProbeDriver.scala#L76
)

The `RobotProveDriver` also has `openProject` method, which is a wrapper around the regular `openProject` on `ProbeDriver`.
It adds more advanced actions during waiting for project open, like monitoring the Build panel for results or closing
the *Tip of the Day* modal which can't be easily done without robot.

```scala
import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.Test
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.robot.RobotPluginExtension

class ExampleRobotTest extends IdeProbeFixture with RobotPluginExtension {
  private val fixture = fixtureFromConfig("example.conf")

  @Test def test() = {
    fixture.run { intelliJ =>
      val buildSbt = intelliJ.workspace.resolve("build.sbt")
      buildSbt.write(""" name := "example" """)

      // Open project with more advanced features
      intelliJ.probe.withRobot.openProject(intelliJ.workspace)

      // use JetBrains Remote-Robot directly
      val robot: RemoteRobot = intelliJ.probe.withRobot.robot
      val image = robot.getScreenshot()
    }
  }

}
```

# Waiting

## Background
It is frequently required to wait after interacting with IDE, for example after invoking an action 
or opening the project. 

Let's look closer at project opening. The internal API of IntelliJ returns early, before most of 
the projects would be imported, it invokes multiple background tasks, to call to the build tool
and synchronize the project, additionally it performs indexing. Before indexing is complete, a lot
of actions are disabled. For this reason it is best to wait for all background tasks to be complete.

## Constructing WaitLogic

The below snippet contains a couple of examples of creating a `WaitLogic`. 

```scala
// Waits till list of background tasks is empty, taking into the account
// only the tasks that have name and display in the UI properly.
// In this example it overrides the default 10 minute waiting limit.
// It is the default waiting method used during most of ide-probe endpoints.
WaitLogic.emptyNamedBackgroundTasks(atMost = 20.minutes)

// Waits for a background task with name containing "indexing" (case sensitive),
// to start and finish, i.e. it makes sure the task actually started and completed.
WaitLogic.backgroundTaskCompletes("indexing")

// Waits until project named "example" exists.
WaitLogic.projectByName("example")

// It is possible to perform some actions during waiting, which is useful 
// when it is long, or if some additional condition can be checked.
// This code tries to close *Tip of the Day*. Calling `DoOnlyOnce.attempt`,
// ensures that the code will have at most one successful run.
val closeTip = new DoOnlyOnce(closeTipOfTheDay())
WaitLogic.emptyNamedBackgroundTasks().doWhileWaiting {
   closeTip.attempt()
   checkBuildPanelErrors()
}
```

## Using WaitLogic
All relevant methods that need waiting accept optional parameter of type `WaitLogic` (for example
`invokeAction`, `openProject`), which is a way to override the default.

```scala
// Extend waiting limit for opening project and set checking frequency to 10 seconds.
val waitLogic = WaitLogic.emptyNamedBackgroundTasks(atMost = 30.minutes, basicCheckFrequency = 10.seconds)
intelliJ.probe.openProject(path, waitLogic)
```

There is also a special method called `await`, that just executes the provided `WaitLogic`.

The example below is a code from bazel extension that is responsible for building bazel project,
using its custom action. It invokes the build action, constructs `WaitLogic` that waits for
the build result (internally implemented as reading bazel console output using `RemoteRobot`),
and finally, waits using this logic through `await` method.

```scala
intelliJ.probe.invokeActionAsync("MakeBlazeProject")

var result = Option.empty[BazelBuildResult]
val buildWait = WaitLogic.basic(checkFrequency = checkFrequency, atMost = waitLimit) {
  findBuildResult() match {
    case buildResult @ Some(_) =>
      result = buildResult
      WaitDecision.Done
    case None => 
       WaitDecision.KeepWaiting("Waiting for bazel build to complete")
  }
}

intelliJ.probe.await(buildWait)
```

# Showcase

Probe is currently being actively used in:

1. [IntelliJ Pants plugin](https://github.com/pantsbuild/intellij-pants-plugin)

It discovered or reproduced the following issues:

1. Failing to import SBT projects without any JDK
   specified [pull request](https://github.com/JetBrains/intellij-scala/pull/562)
2. Malfunctioning VCS root detection for pants plugin
3. Missing thrift-related objects in the find window
4. Failing to import pants project using BSP
5. Incorrect pants project name generation
6. Problematic conflict when handling BUILD files with both Bazel and Pants plugin installed
7. BSP project performance regression in IntelliJ 2020.3
