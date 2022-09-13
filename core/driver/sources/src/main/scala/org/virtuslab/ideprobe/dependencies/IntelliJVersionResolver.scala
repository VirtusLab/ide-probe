package org.virtuslab.ideprobe.dependencies

import java.nio.file.Path

import com.google.gson.JsonParser

import org.virtuslab.ideprobe.Extensions._

object IntelliJVersionResolver {
  def version(intelliJPath: Path): IntelliJVersion = {
    val productInfo = intelliJPath.resolve("product-info.json").content()
    val productInfoJsonObject = JsonParser.parseString(productInfo).getAsJsonObject
    val version = productInfoJsonObject.getAsJsonPrimitive("version").getAsString
    val buildNumber = productInfoJsonObject.getAsJsonPrimitive("buildNumber").getAsString

    IntelliJVersion(buildNumber, Some(version), ext = None)
  }
}
