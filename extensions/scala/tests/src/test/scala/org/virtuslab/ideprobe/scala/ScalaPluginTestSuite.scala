package org.virtuslab.ideprobe.scala

import org.virtuslab.ideprobe.junit4.IdeProbeTestSuite
import org.virtuslab.ideprobe.robot.RobotPluginExtension

class ScalaPluginTestSuite
    extends IdeProbeTestSuite
    with ScalaPluginExtension
    with BloopExtension
    with RobotPluginExtension

object ScalaPluginTestSuite extends ProbeDriverTestParamsProvider