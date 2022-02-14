package omega.scaladsl.api

import omega.scaladsl.api.Change.Result

trait Session {
  def push(s: String): Result
  def insert(s: String, offset: Long): Result
  def overwrite(s: String, offset: Long): Result
  def delete(offset: Long, len: Long): Result

  def view(offset: Long, size: Long): Viewport
  def viewCb(offset: Long, size: Long, cb: ViewportCallback): Viewport

  def findChange(id: Long): Option[Change]
}
