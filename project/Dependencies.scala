import net.aichler.jupiter.sbt.Import.JupiterKeys
import sbt._

object Dependencies {

  val junit = Seq(
    "junit" % "junit" % "4.12" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  val nuProcess = "com.zaxxer" % "nuprocess" % "1.2.6"

  val remoteRobot = "com.intellij.remoterobot" % "remote-robot" % "0.9.35"

  val remoteRobotFixtures = "com.intellij.remoterobot" % "remote-fixtures" % "1.1.18"

  val gson = "com.google.code.gson" % "gson" % "2.8.6"

  // because idea plugin packager would only take the root jar which has no classes
  // somehow it fails to see the transitive dependencies (even though the code says it should)
  // so here are all the dependencies explicitly
  val pureConfig = {
    val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
    val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
    val pureConfigModules =
      Seq("pureconfig", "pureconfig-macros", "pureconfig-generic", "pureconfig-generic-base", "pureconfig-core")
    pureConfigModules.map { module =>
      "com.github.pureconfig" %% module % "0.12.2"
    } ++ Seq(typesafeConfig, shapeless)
  }

  val junit5 = Seq(
    "org.junit.jupiter" % "junit-jupiter-params" % "5.4.2",
    "net.aichler" % "jupiter-interface" % "0.8.3" % Test
  )

  val intellijScala = "org.intellij.scala:2020.2.753:nightly"

}
