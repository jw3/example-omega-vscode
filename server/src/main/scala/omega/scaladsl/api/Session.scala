package omega.scaladsl.api

import omega.scaladsl.ViewportCallback

trait Session {
  def push(s: String): Unit
  def overwrite(s: String, offset: Long): Unit
  def delete(offset: Long, len: Long): Unit

  def view(offset: Long, size: Long): Viewport
  def viewCb(offset: Long, size: Long, cb: ViewportCallback): Viewport
}
