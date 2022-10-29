package org.virtuslab.ideprobe.dependencies

import java.nio.file.Path

import com.google.gson.JsonParser

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.OS

object IntelliJVersionResolver {
  def version(intelliJPath: Path): IntelliJVersion = {
    val productInfo =
      if (OS.Current == OS.Mac && intelliJPath.name == "Contents")
        intelliJPath.resolve("Resources").resolve("product-info.json").content()
      else
        intelliJPath.resolve("product-info.json").content()
    val productInfoJsonObject = JsonParser.parseString(productInfo).getAsJsonObject
    val version = productInfoJsonObject.getAsJsonPrimitive("version").getAsString
    val buildNumber = productInfoJsonObject.getAsJsonPrimitive("buildNumber").getAsString

    IntelliJVersion(buildNumber, Some(version), ext = "")
  }
}
