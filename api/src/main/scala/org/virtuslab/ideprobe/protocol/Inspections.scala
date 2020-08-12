package org.virtuslab.ideprobe.protocol

sealed trait RunFixesSpec
object RunFixesSpec {
  object All extends RunFixesSpec
  object None extends RunFixesSpec
  case class Specific(fixes: Seq[String]) extends RunFixesSpec
}

case class InspectionRunParams(
  className: String,
  targetFile: FileRef,
  runFixes: RunFixesSpec
)

case class ProblemDescriptor(message: String, line: Int, element: String, fixes: Seq[String])
case class InspectionRunResult(descriptors: Seq[ProblemDescriptor])
