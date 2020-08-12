package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

case class FileRef(path: Path, project: ProjectRef = ProjectRef.Default)
