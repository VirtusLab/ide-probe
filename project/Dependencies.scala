import sbt._

object Dependencies {

  val junit: Seq[ModuleID] = Seq(
    "junit" % "junit" % "4.13.2" % Test,
    ("com.github.sbt" % "junit-interface" % "0.13.3" % Test).exclude("junit", "junit-dep")
  )

  val junitCompile: Seq[ModuleID] = Seq(
    "junit" % "junit" % "4.13.2" % Compile
  )

  def scalaLib(version: String): Seq[ModuleID] = Seq(
    "org.scala-lang" % "scala-library" % version
  )

  val commonsCodec = "commons-codec" % "commons-codec" % "1.16.1"
  val commonsIO = "commons-io" % "commons-io" % "2.15.0"

  val nuProcess = "com.zaxxer" % "nuprocess" % "2.0.6"

  val jGit = "org.eclipse.jgit" % "org.eclipse.jgit" % "6.7.0.202309050840-r"

  val remoteRobot = "com.intellij.remoterobot" % "remote-robot" % "0.11.22"

  val remoteRobotFixtures = "com.intellij.remoterobot" % "remote-fixtures" % "0.11.22"

  val gson = "com.google.code.gson" % "gson" % "2.10.1"

  val jsoup = "org.jsoup" % "jsoup" % "1.17.2"

  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0" % Compile

  // because idea plugin packager would only take the root jar which has no classes
  // somehow it fails to see the transitive dependencies (even though the code says it should)
  // so here are all the dependencies explicitly
  val pureConfig: Seq[ModuleID] = {
    val typesafeConfig = "com.typesafe" % "config" % "1.4.3"
    val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
    val pureConfigModules =
      Seq("pureconfig", "pureconfig-generic", "pureconfig-generic-base", "pureconfig-core")
    pureConfigModules.map { module =>
      "com.github.pureconfig" %% module % "0.17.6"
    } ++ Seq(typesafeConfig, shapeless)
  }

  val intellijScala = "org.intellij.scala:2020.2.753:nightly"

}
