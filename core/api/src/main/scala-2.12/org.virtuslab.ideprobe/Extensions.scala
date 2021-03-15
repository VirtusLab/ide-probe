package org.virtuslab.ideprobe

import scala.collection.convert.DecorateAsJava
import scala.collection.convert.DecorateAsScala

object Extensions extends ProbeExtensions with DecorateAsJava with DecorateAsScala {

  implicit class DistinctBySeq[A](val s: Seq[A]) extends AnyVal {
    def distinctBy[B](property: A => B): Seq[A] = {
      s.groupBy(property).map(_._2.head)(collection.breakOut)
    }
  }

}
