package com.twitter.intellij.pants

import java.nio.file.{Files, Path}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.{ConfigFormat, IntelliJFixture}

object FastpassSetup extends ConfigFormat with BspFixture {

  def overrideFastpassVersion(fixture: IntelliJFixture, workspace: Path): Unit = {
    fixture.config.get[String]("fastpass.version").foreach { version =>
      setupFromVersionString(workspace, version)
    }
  }

  private def setupFromVersionString(workspace: Path, version: String): Unit = {
    val fastpass = workspace.resolve("fastpass/bin/fastpass")
    if (Files.exists(fastpass)) {
      val content =
        s"""#!/usr/bin/env bash
           |$coursierPath launch org.scalameta:fastpass_2.12:$version --quiet --main scala.meta.fastpass.Fastpass -- "$$@"
           |""".stripMargin
      fastpass.write(content)
    }
  }

}
