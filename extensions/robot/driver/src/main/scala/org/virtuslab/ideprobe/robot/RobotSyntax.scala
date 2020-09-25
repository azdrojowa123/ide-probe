package org.virtuslab.ideprobe.robot

import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.fixtures.{CommonContainerFixture, Fixture}
import com.intellij.remoterobot.search.locators.Locators
import java.time.Duration
import org.virtuslab.ideprobe.Extensions._

trait SearchableComponent {
  protected def searchContext: SearchContext
  protected def robotTimeout: Duration

  def find(xpath: String): CommonContainerFixture = {
    searchContext.find(classOf[CommonContainerFixture], Locators.byXpath(xpath), robotTimeout)
  }

  def findAll(xpath: String): Seq[CommonContainerFixture] = {
    searchContext.findAll(classOf[CommonContainerFixture], Locators.byXpath(xpath)).asScala.toList
  }

  def findOpt(xpath: String): Option[CommonContainerFixture] = {
    findAll(xpath) match {
      case Seq()       => None
      case Seq(single) => Some(single)
      case many        => throw new RuntimeException(s"Found multiple elements for query $xpath: $many")
    }
  }

  def mainWindow: CommonContainerFixture = find(RobotSyntax.query.className("IdeFrameImpl"))
}

object RobotSyntax extends RobotSyntax

trait RobotSyntax { outer =>
  val robotTimeout: Duration = Duration.ofSeconds(10)

  implicit class SearchableOps(val searchContext: SearchContext) extends SearchableComponent {
    override protected def robotTimeout: Duration = outer.robotTimeout
  }

  implicit class FixtureOps(val fixtureComponent: Fixture) {
    def fullText: String = fullTexts.mkString("\n")
    def fullTexts: Seq[String] = fixtureComponent.findAllText.asScala.map(_.getText).toList
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
