package org.virtuslab.ideprobe.handlers

import java.util.logging.Logger
import scala.reflect.ClassTag

trait ScalaReflectionApi {

  private final val logger = Logger.getLogger("ScalaReflectionApi")

  import scala.reflect.runtime.{universe => ru}
  private final val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)

  def instance(className: String): Any = {
    val module = runtimeMirror.staticModule(className)
    val obj = runtimeMirror.reflectModule(module)
    obj.instance
  }

  def constructor(className: String, args: Any*): ru.MethodMirror = {
    val cls = runtimeMirror.staticClass(className)
    val obj = runtimeMirror.reflectClass(cls)
    val constructorMember = obj.symbol.info.member(ru.termNames.CONSTRUCTOR)
    val constructorSymbol = findMethod(constructorMember, args)
    obj.reflectConstructor(constructorSymbol)
  }

  def method(className: String, methodName: String, args: Any*): ru.MethodMirror = {
    val module = runtimeMirror.staticModule(className)
    val obj = runtimeMirror.reflectModule(module)
    val methodModel = obj.symbol.info.member(ru.TermName(methodName))
    val methodSymbol = findMethod(methodModel, args)
    val im = runtimeMirror.reflect(obj.instance)
    val method = im.reflectMethod(methodSymbol)
    method
  }

  implicit class ScalaReflectionOps[A: ClassTag](obj: A) {

    def withScalaReflection: ScalaReflectionOps = new ScalaReflectionOps(obj)

    class ScalaReflectionOps(obj: A) {

      def method(methodName: String, args: Any*): ru.MethodMirror = {
        val im = runtimeMirror.reflect(obj)
        val methodMember = im.symbol.info.member(ru.TermName(methodName))
        val methodSymbol = findMethod(methodMember, args)
        val method = im.reflectMethod(methodSymbol)
        method
      }

      def getField(fieldName: String): Any = {
        import scala.reflect.runtime.universe
        val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
        val objReflect = runtimeMirror.reflect(obj)
        val method = objReflect.symbol.info.member(universe.TermName(fieldName)).asTerm
        objReflect.reflectField(method).get
      }

      def setField(fieldName: String)(value: Any): Unit = {
        import scala.reflect.runtime.universe
        val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
        val objReflect = runtimeMirror.reflect(obj)
        val method = objReflect.symbol.info.member(universe.TermName(fieldName)).asTerm
        objReflect.reflectField(method).set(value)
      }
    }
  }

  private def findMethod(symbol: ru.Symbol, args: Any*): ru.MethodSymbol = {
    logger.severe(s"logger:$symbol,args:$args")
    if (args.head.asInstanceOf[List[_]].isEmpty)
      symbol.alternatives.head.asMethod
    else
      symbol.alternatives.find{m => m.isMethod && m.info.typeArgs == args.map(getTypeTag)}.getOrElse(symbol.alternatives.head).asMethod
  }

  private def getTypeTag[T: ru.TypeTag](obj: T): ru.TypeTag[T] = ru.typeTag[T]

}
