package dynver
package impl

import scala.sys.process.ProcessLogger

object NoProcessLogger extends ProcessLogger {
  def info(s: => String)  = ()
  def out(s: => String)   = ()
  def error(s: => String) = ()
  def err(s: => String)   = ()
  def buffer[T](f: => T)  = f
}
