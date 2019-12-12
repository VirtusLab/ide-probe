package com.virtuslab.ideprobe

import java.lang.reflect.Method

final case class TestCase(suite: String, name: String)

object TestCase {
  def current: Option[TestCase] = {
    Thread
      .currentThread()
      .getStackTrace
      .flatMap(matchingMethods)
      .find(_.getDeclaredAnnotations.map(_.annotationType().getName).contains("org.junit.Test"))
      .map(method => TestCase(method.getDeclaringClass.getName, method.getName))
  }

  def matchingMethods(trace: StackTraceElement): Seq[Method] =
    try {
      Class.forName(trace.getClassName).getMethods.filter(_.getName == trace.getMethodName)
    } catch {
      case _: ReflectiveOperationException => Nil
    }
}
