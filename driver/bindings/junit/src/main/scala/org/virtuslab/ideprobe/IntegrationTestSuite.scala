package org.virtuslab.ideprobe

import java.util.concurrent.Executors
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@RunWith(classOf[JUnit4])
trait IntegrationTestSuite {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def resourcePath(name: String): String = {
    s"${getClass.getSimpleName}/$name.conf"
  }

  final def fixtureFromConfig(name: String = "ideprobe"): IntelliJFixture = {
    fixtureFromConfig(Config.fromClasspath(resourcePath(name)))
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
