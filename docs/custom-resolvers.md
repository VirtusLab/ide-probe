#### Custom resolvers
Ide Probe can download its dependencies from custom repositories. 
If none of those is specified, official ones will be used.   

##### IntelliJ resolver
```
probe.resolvers {
  intellij.repository {
    uri = "https://www.jetbrains.com/intellij-repository/snapshots"
    group = "com.jetbrains.intellij.idea"
    artifact = "ideaIC"
  }
  plugins.repository {
    uri = "https://plugins.jetbrains.com/plugin/download"
  }
}
```

Ide probe can fetch Intellij from a maven repository or use a custom plugin repository.

##### Resource cache
```
probe.resolvers {
  resourceProvider.cacheDir = "/tmp/ideprobe/cache"
}
```

All resources not present on the local machine will be cached in the 'cacheDir' directory.