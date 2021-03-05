package org.virtuslab.ideprobe
package robot

import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.fixtures.{CommonContainerFixture, Fixture}
import com.intellij.remoterobot.search.locators.Locators
import java.time.Duration
import org.virtuslab.ideprobe.Extensions._
import scala.concurrent.duration._

trait SearchableComponent {
  protected def searchContext: SearchContext
  protected def robotTimeout: FiniteDuration

  def findWithTimeout(xpath: String, timeout: FiniteDuration): CommonContainerFixture = {
    searchContext.find(
      classOf[CommonContainerFixture],
      Locators.byXpath(xpath),
      Duration.ofMillis(timeout.toMillis)
    )
  }

  def find(xpath: String): CommonContainerFixture = {
    findWithTimeout(xpath, timeout = robotTimeout)
  }

  def findAll(xpath: String): Seq[CommonContainerFixture] = {
    searchContext.findAll(classOf[CommonContainerFixture], Locators.byXpath(xpath)).asScala.toList
  }

  def findOpt(xpath: String): Option[CommonContainerFixture] = {
    findAll(xpath) match {
      case Seq()       => None
      case Seq(single) => Some(single)
      case many        => error(s"Found multiple elements for query $xpath: $many")
    }
  }

  def mainWindow: CommonContainerFixture = find(RobotSyntax.query.className("IdeFrameImpl"))
}

object RobotSyntax extends RobotSyntax

trait RobotSyntax { outer =>
  val robotTimeout: FiniteDuration = 10.seconds

  implicit class SearchableOps(val searchContext: SearchContext) extends SearchableComponent {
    override protected def robotTimeout: FiniteDuration = outer.robotTimeout
  }

  implicit class FixtureOps(val fc: Fixture) {
    def fullText: String = fullTexts.mkString("\n")
    def fullTexts: Seq[String] = fc.findAllText.asScala.map(_.getText).toList
    def doClick(): Unit = fc.runJs("component.doClick();", true)
    def setText(text: String): Unit = fc.runJs(s"component.setText('$text');", true)
  }

  object query {
    def dialog(title: String): String = {
      dialog("title" -> title)
    }
    def dialog(attributes: (String, String)*): String = {
      val allAttrs = ("class" -> "MyDialog") +: attributes
      div(allAttrs: _*)
    }
    def button(attributes: (String, String)*): String = {
      val allAttrs = ("class" -> "JButton") +: attributes
      div(allAttrs: _*)
    }
    def button(accessibleName: String, attributes: (String, String)*): String = {
      val allAttrs = ("accessiblename" -> accessibleName) +: attributes
      button(allAttrs: _*)
    }
    def radioButton(attributes: (String, String)*): String = {
      val allAttrs = ("class" -> "JRadioButton") +: attributes
      div(allAttrs: _*)
    }
    def radioButton(accessibleName: String, attributes: (String, String)*): String = {
      val allAttrs = ("accessiblename" -> accessibleName) +: attributes
      radioButton(allAttrs: _*)
    }
    def className(name: String): String = {
      div("class" -> name)
    }
    def div(attributes: (String, String)*): String = {
      attributes.map { case (name, value) => s"@$name='$value'" }.mkString("//div[", " and ", "]")
    }
  }
}
