#### Workspace

User can use workspace to predefine any data required by a single test case.
     
It can be defined as one of:

1. Directory on the filesystem
`probe.workspace.path = "file://home/foo/bar"`

2. Directory on the classpath

`probe.workspace.path = "classpath:/foo/bar"`
3. Directory within a jar
 
`probe.workspace.path = "jar:file://foo.jar!/bar/baz"`

4. Github repository

`probe.workspace.path = "https://github.com/foo/bar"`

```
probe.workspace {
    path = "https://github.com/foo/bar"`
    branch = foo
}
```

```
probe.workspace {
    path = "https://github.com/foo/bar"`
    tag = foo-1.0
}
```

```
probe.workspace {
    path = "https://github.com/foo/bar"`
    commit = 4a2b9edc84b15f4a4707fe75e5036ee5d12f7ac4
}
```
