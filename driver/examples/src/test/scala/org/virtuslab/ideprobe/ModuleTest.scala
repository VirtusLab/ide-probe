package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

abstract class ModuleTest {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  private val configs = Seq("SbtProject/shapeless.conf", "SbtProject/cats.conf", "SbtProject/dokka.conf")
  protected val fixtures = configs.map(Config.fromClasspath).map(IntelliJFixture.fromConfig(_))
}
