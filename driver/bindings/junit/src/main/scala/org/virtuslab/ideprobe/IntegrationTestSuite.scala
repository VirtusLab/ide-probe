package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

@RunWith(classOf[JUnit4])
trait IntegrationTestSuite {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def resolveConfig(): Config = {
    val className = getClass.getSimpleName.stripSuffix("$")
    val inDirectory = s"$className/ideprobe.conf"
    val inFile = s"$className.conf"
    Try(Config.fromClasspath(inFile)).getOrElse(Config.fromClasspath(inDirectory))
  }

  final def fixtureFromConfig(): IntelliJFixture = {
    fixtureFromConfig(resolveConfig())
  }

  final def fixtureFromConfig(config: Config): IntelliJFixture = {
    transformFixture(IntelliJFixture.fromConfig(config))
  }

  protected def transformFixture(fixture: IntelliJFixture): IntelliJFixture = fixture

  def within(limit: FiniteDuration, interval: FiniteDuration = 100.millis)(block: => Unit): Unit = {
    lazy val start = System.nanoTime()

    def timeLimitExceeded: Boolean =
      limit.toNanos < (System.nanoTime() - start)

    @tailrec
    def loop(): Unit = {
      try {
        block
      } catch {
        case e: Throwable =>
          if (timeLimitExceeded) {
            throw e
          } else {
            Thread.sleep(interval.toMillis)
            loop()
          }
      }
    }

    loop()
  }

}
