package org.virtuslab.ideprobe.protocol

final case class ExpandMacroData(
    fileRef: FileRef,
    macroText: String
)
