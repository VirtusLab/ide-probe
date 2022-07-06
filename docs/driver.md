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
driver.check {
    errors {
       enabled = false
       excludeMessages = []
       includeMessages = [".*"]
     }
    freezes {
       enabled = false
    }
}
```

By default, the driver doesn't fail the test upon detecting any errors or freezes during the execution.
Field `excludeMessages` is an empty list by default. Putting some regular expressions into this field will cause probe to ignore IDE errors whose messages contain a substring matching any of the given expressions, even if `probe.driver.check.errors.enabled` is `true`.
By default, field `includeMessages` contains one regular expression, that matches all IDE errors messages. If you want to include only errors whose substrings match certain regular expressions, you have to
put them into this field. 

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