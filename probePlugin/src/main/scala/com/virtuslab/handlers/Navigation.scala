package com.virtuslab.handlers
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.openapi.project.Project
import com.virtuslab.ideprobe.protocol.NavigationQuery
import com.virtuslab.ideprobe.protocol.NavigationTarget
import scala.collection.mutable

object Navigation extends IntelliJApi {
  def find(query: NavigationQuery): List[NavigationTarget] = {
    val project = Projects.resolve(query.project)
    val all = mutable.Buffer[NavigationTarget]()

    ChooseByNameContributor.CLASS_EP_NAME.forEachExtensionSafe { contributor =>
      findItems(query, project, contributor)
        .map(item => NavigationTarget(item.getName, item.getPresentation.getLocationString))
        .foreach(all.addOne)
    }

    all.toList
  }

  private def findItems(query: NavigationQuery, project: Project, contributor: ChooseByNameContributor) = {
    read(contributor.getItemsByName(query.value, "", project, false))
  }
}
