By using this library you implicitly accept the terms of the Jetbrains Privacy Policy.

#### Description

Ide Probe is a framework for testing IntelliJ platform plugins. It can be used both locally and in the CI pipeline. 

The framework itself comprises two components: 
- driver - responsible for controlling the workspace and IDE startup
- probe - used to interact with the IDE  

#### Overview

A single test case consists of a configuration and workflow specification. 
Configuration can either be:

A) loaded from file,
```scala
private val config = Config.fromClasspath("path/to/file")

@Test def test = IntelliJFixture.fromConfig(config).run {intelliJ => 
    // workflow steps
}
```
B) provided as a string, or
```scala
private val config = Config.fromString("""probe { workspace.path = /foo/bar } """)
@Test def test = IntelliJFixture.fromConfig(config).run {intelliJ => 
    // workflow steps
}
```
C) specified programmatically.
```scala
private val fixture = IntelliJFixture(
  workspaceTemplate = WorkspaceTemplate.fromFile(path),
  version = IntelliJVersion("202.5792.28-EAP-SNAPSHOT"),
  plugins = List(Plugin("org.intellij.scala", "2020.2.7"))
)

@Test def test = fixture.run {intelliJ => 
    // workflow steps
} 
```

Workflow can only be defined programmatically, since it comprises sequence of intertwined:
1. probe commands,
2. IDE state queries,
3. workspace manipulation,
4. custom verification logic.

```scala
@Test def test = fixture.run { intelliJ =>
  val buildSbt = intelliJ.workspace.resolve("build.sbt")
  Files.write(buildSbt, """name := "foo" """)

  val projectRef = intelliJ.probe.openProject(buildSbt)
  val structure = intelliJ.probe.projectModel(projectRef)

  assertEquals("foo", structure.name)  
}
``` 

To see the full list of probe endpoints see 
[Commands](docs/endpoints/commands.md) or [Queries](docs/endpoints/queries.md).

Note, that any communication with the probe is synchronous.

#### Configuration 

1. [Driver](docs/driver.md)
2. [Resolvers](docs/custom-resolvers.md)
3. [Workspace](docs/workspace.md)
4. [Display](docs/display.md)
5. [Debugging](docs/debug.md)
