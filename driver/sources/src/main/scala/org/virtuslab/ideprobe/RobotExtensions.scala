package org.virtuslab.ideprobe

import java.time.Duration

import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.Fixture
import com.intellij.remoterobot.search.locators.Locators
import Extensions._

trait RobotExtensions {
  protected val robotTimeout = Duration.ofSeconds(10)

  implicit class SearchableOps(val component: SearchContext) {
    def find(xpath: String): CommonContainerFixture = {
      component.find(classOf[CommonContainerFixture], Locators.byXpath(xpath), robotTimeout)
    }

    def findAll(xpath: String): Seq[CommonContainerFixture] = {
      component.findAll(classOf[CommonContainerFixture], Locators.byXpath(xpath)).asScala.toList
    }

    def findOpt(xpath: String): Option[CommonContainerFixture] = {
      findAll(xpath) match {
        case Seq() => None
        case Seq(single) => Some(single)
        case many => throw new RuntimeException(s"Found multiple elements for query $xpath: $many")
      }
    }

    def mainWindow: CommonContainerFixture = find(query.className("IdeFrameImpl"))
  }

  implicit class FixtureOps(val component: Fixture) {
    def fullText: String = fullTexts.mkString("\n")
    def fullTexts: Seq[String] = component.findAllText.asScala.map(_.getText).toList
  }

  object query {
    def dialog(title: String): String = {
      div("class" -> "MyDialog", "title" -> title)
    }
    def className(name: String): String = {
      div("class" -> name)
    }
    def div(attributes: (String, String)*): String = {
      attributes.map { case (name, value) => s"@$name='$value'" }.mkString("//div[", " and ", "]")
    }
  }
}

object RobotExtensions extends RobotExtensions
