import sbt._

object Dependencies {

  val junit = Seq(
    "junit" % "junit" % "4.13.2" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  val junitCompile = Seq(
    "junit" % "junit" % "4.13.2" % Compile
  )

  def scalaLib(version: String) = Seq(
    "org.scala-lang" % "scala-library" % version
  )

  val nuProcess = "com.zaxxer" % "nuprocess" % "2.0.3"

  val remoteRobot = "com.intellij.remoterobot" % "remote-robot" % "0.11.14"

  val remoteRobotFixtures = "com.intellij.remoterobot" % "remote-fixtures" % "0.11.14"

  val gson = "com.google.code.gson" % "gson" % "2.9.0"

  // because idea plugin packager would only take the root jar which has no classes
  // somehow it fails to see the transitive dependencies (even though the code says it should)
  // so here are all the dependencies explicitly
  val pureConfig = {
    val typesafeConfig = "com.typesafe" % "config" % "1.4.2"
    val shapeless = "com.chuusai" %% "shapeless" % "2.3.9"
    val pureConfigModules =
      Seq("pureconfig", "pureconfig-generic", "pureconfig-generic-base", "pureconfig-core")
    pureConfigModules.map { module =>
      "com.github.pureconfig" %% module % "0.17.1"
    } ++ Seq(typesafeConfig, shapeless)
  }

  val intellijScala = "org.intellij.scala:2020.2.753:nightly"

}
