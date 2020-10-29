resolvers += Resolver.jcenterRepo // for junit 5 support

addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "3.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.8.3")
addSbtPlugin("com.lucidchart" % "sbt-cross" % "4.0")