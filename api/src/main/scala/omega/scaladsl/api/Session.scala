package omega.scaladsl.api

trait Session {
  def push(s: String): ChangeResult
  def insert(s: String, offset: Long): ChangeResult
  def overwrite(s: String, offset: Long): ChangeResult
  def delete(offset: Long, len: Long): ChangeResult

  def view(offset: Long, size: Long): Viewport
  def viewCb(offset: Long, size: Long, cb: ViewportCallback): Viewport

  def findChange(id: Long): Option[Change]
}
