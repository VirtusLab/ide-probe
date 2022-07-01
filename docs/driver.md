### Driver

Driver controls launching the IDE  

#### launch Command
`driver.launch.command = ["idea"]`

The command used to launch the IDE. By default, the driver uses the `idea` command, but user can specify a custom command. Note, that the `idea` is available on the `PATH`, if the user wants to wrap it in some custom logic. 
##### launch Timeout
`driver.launch.timeout = "30 seconds"`

By default, the driver waits `30 seconds` for the IDE to connect the probe. After exceeding this time, the whole test fails.  
#### Automatic checks
```
driver.checks {
    errors = false
    freezes = false
}
```

By default, the driver doesn't fail the test upon detecting any errors or freezes during the execution.
  

#### Headless mode
`driver.headless = false`

By default, the driver launches the IDE in the non-headless mode. Note, that the behavior of the IDE can differ between headless and non-headless modes.  

#### Virtual Machine options
`driver.vmOptions = ["-Xmx4096m"]`
Empty by default. Used by the driver to further customize the IDE.

#### Screen size
```
driver {
  xvfb {
    screen {
      width = 1920
      height = 1080
      depth = 24
    }
  }
}
```
Used to configure screen size in Xvfb display mode. 