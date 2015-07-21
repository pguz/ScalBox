package common

import org.slf4j.LoggerFactory

object LoggerAspect {
  def apply(clazz: Class[_]) = new LoggerAspect(clazz)
}

class LoggerAspect(clazz: Class[_]) {
  private val log = LoggerFactory.getLogger(clazz)

  private def logResAspect[A](x: A)(g: String => Unit): A = {g(x.toString); x}
  private def logDescAspect[A](x: A)(g: Unit => Unit): A = {g(); x}

  def info(s: String)                   = log.info(s)
  def infoResAsp[A](x: A)               = logResAspect(x)(log.info)
  def infoDescAsp[A](s: String, x: A)   = logDescAspect(x)(_ => log.info(s))
  def infoAsp[A](s: String, x: A)       = logResAspect(x){ y => log.info(s + " => " + y) }

  def debug(s: String)                  = log.debug(s)
  def debugResAsp[A](x: A)              = logResAspect(x)(log.debug)
  def debugDescAsp[A](s: String, x: A)  = logDescAspect(x)(_ => log.debug(s))
  def debugAsp[A](s: String, x: A)      = logResAspect(x){ y => log.debug(s + " => " + y) }

  def trace(s: String)                  = log.trace(s)
  def traceResAsp[A](x: A)              = logResAspect(x)(log.trace)
  def traceDescAsp[A](s: String, x: A)  = logDescAspect(x)(_ => log.trace(s))
  def traceAsp[A](s: String, x: A)      = logResAspect(x){ y => log.trace(s + " => " + y) }

  def warn(s: String)                   = log.warn(s)
  def warnResAsp[A](x: A)               = logResAspect(x)(log.warn)
  def warnDescAsp[A](s: String, x: A)   = logDescAspect(x)(_ => log.warn(s))
  def warnAsp[A](s: String, x: A)       = logResAspect(x){ y => log.warn(s + " => " + y) }

  def error(s: String, err: Throwable)  = log.error(s, err)
  def errorResAsp[A](x: A)              = logResAspect(x)(log.error)
  def errorDescAsp[A](s: String, err: Throwable, x: A)
    = logDescAspect(x)(_ => log.error(s, err))
  def errorAsp[A](s: String, err: Throwable, x: A)
    = logResAspect(x){ y => log.error(s + " => " + y, err) }
}
