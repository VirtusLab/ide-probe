scalaVersion := "2.13.1"
name := "simple-sbt-project"
organization := "ch.epfl.scala"
version := "1.0"

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % Test,
  ("com.github.sbt" % "junit-interface" % "0.13.3" % Test).exclude("junit", "junit-dep")
)
