package org.virtuslab.ideprobe.handlers

import java.nio.file.Path
import java.nio.file.Paths

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.CheckBoxList
import javax.swing.JList
import org.virtuslab.ideprobe.Probe

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

object IntelliJApi extends IntelliJApi

trait IntelliJApi {
  def runOnUIAsync(block: => Unit): Unit = {
    application.invokeLater(() => block)
  }

  def runOnUISync[A](block: => A): A = {
    var result = Option.empty[A]
    application.invokeAndWait(() => result = Some(block))
    result.get
  }

  def write[A](block: => A): A = {
    application.runWriteAction(new ThrowableComputable[A, Throwable] {
      override def compute(): A = block
    })
  }

  def read[A](block: => A): A = {
    application.runReadAction(new ThrowableComputable[A, Throwable] {
      override def compute(): A = block
    })
  }

  protected val log: Logger = Logger.getInstance(classOf[Probe])

  protected def logsPath: Path = Paths.get(PathManager.getLogPath)

  protected def application: Application = ApplicationManager.getApplication

  protected def error(message: String): Nothing = {
    throw new RuntimeException(message)
  }

  protected def await[A](future: Future[A], duration: Duration = Duration.Inf): A = {
    Await.result(future, duration)
  }

  implicit class UserDataHolderOps(val holder: UserDataHolder) {
    def getAndClearUserData[A](key: Key[A]): A = {
      val result = holder.getUserData(key)
      holder.putUserData(key, null.asInstanceOf[A])
      result
    }
  }

  implicit class ReflectionOps[A](obj: A) {
    import java.lang.reflect._
    def invoke[B: ClassTag](name: String)(args: Object*): B = {
      val params = args.map(_.getClass)
      val method = getMethod(obj.getClass, name, params: _*)

      val result = using(method) { method =>
        if (params.isEmpty) method.invoke(obj)
        else method.invoke(obj, args: _*)
      }
      result.asInstanceOf[B]
    }

    def field[B: ClassTag](name: String): B = {
      val field = getField(obj.getClass, "my" + name.capitalize)
      using(field)(_.get(obj).asInstanceOf[B])
    }

    private def using[B <: AccessibleObject, C](accessible: B)(f: B => C): C = {
      val isAccessible = accessible.isAccessible
      try {
        accessible.setAccessible(true)
        f(accessible)
      } finally {
        accessible.setAccessible(isAccessible)
      }
    }

    @tailrec
    private def getField(cl: Class[_], name: String): Field = {
      try cl.getDeclaredField(name)
      catch {
        case e: NoSuchFieldException =>
          if (cl.getSuperclass != null) getField(cl.getSuperclass, name) else throw e
      }
    }

    @tailrec
    private def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
      try cl.getDeclaredMethod(name, parameters: _*)
      catch {
        case e: NoSuchMethodException =>
          if (cl.getSuperclass == null) throw e
          else getMethod(cl.getSuperclass, name, parameters: _*)
      }
    }
  }

  implicit class JListOps[A](list: JList[A]) {
    def items: Seq[A] = {
      val listModel = list.getModel
      (0 until listModel.getSize).map(listModel.getElementAt)
    }
  }

  implicit class CheckboxListOps[A](list: CheckBoxList[A]) {
    def items: Seq[A] = {
      (0 until list.getItemsCount).map(list.getItemAt)
    }
  }
}
