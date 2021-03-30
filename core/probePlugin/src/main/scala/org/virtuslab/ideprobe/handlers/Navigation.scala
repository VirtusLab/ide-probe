package org.virtuslab.ideprobe.handlers

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import org.virtuslab.ideprobe.protocol.NavigationQuery
import org.virtuslab.ideprobe.protocol.NavigationTarget

import scala.collection.mutable

object Navigation extends IntelliJApi {
  def find(query: NavigationQuery): List[NavigationTarget] = {
    val project = Projects.resolve(query.project)
    val all = mutable.Buffer[NavigationTarget]()

    ChooseByNameContributor.CLASS_EP_NAME.forEachExtensionSafe { contributor =>
      findItems(query, project, contributor)
        .map(item => read { NavigationTarget(item.getName, item.getPresentation.getLocationString) })
        .foreach(all += _)
    }

    all.toList
  }

  private def findItems(
      query: NavigationQuery,
      project: Project,
      contributor: ChooseByNameContributor
  ): Array[NavigationItem] = {
    read {
      contributor.getItemsByName(query.value, "", project, query.includeNonProjectItems)
    }
  }
}
