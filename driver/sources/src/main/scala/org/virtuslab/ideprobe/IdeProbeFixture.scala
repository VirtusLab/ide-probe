package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.util.Try

trait IdeProbeFixture extends RobotExtensions {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  protected var fixtureTransformers: Seq[IntelliJFixture => IntelliJFixture] = Nil

  /**
   * Default logic for resolving configuration based on test class name
   **/
  def resolveConfig(): Config = {
    val className = getClass.getSimpleName.stripSuffix("$")
    val inDirectory = s"$className/ideprobe.conf"
    val inFile = s"$className.conf"
    Try(Config.fromClasspath(inFile)).getOrElse(Config.fromClasspath(inDirectory))
  }

  final def fixtureFromConfig(): IntelliJFixture = {
    fixtureFromConfig(resolveConfig())
  }

  final def fixtureFromConfig(configClasspathPath: String): IntelliJFixture = {
    fixtureFromConfig(Config.fromClasspath(configClasspathPath))
  }

  final def fixtureFromConfig(config: Config): IntelliJFixture = {
    transformFixture(IntelliJFixture.fromConfig(config))
  }

  protected def registerFixtureTransformer(transformer: IntelliJFixture => IntelliJFixture): Unit = {
    fixtureTransformers :+= transformer
  }

  protected def transformFixture(fixture: IntelliJFixture): IntelliJFixture = {
    fixtureTransformers.foldLeft(fixture)((fixture, transformer) => transformer(fixture))
  }

}
