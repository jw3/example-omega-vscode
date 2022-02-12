package omega.scaladsl

import jnr.ffi.Pointer
import omega.scaladsl.api._

private[scaladsl] class SessionImpl(p: Pointer, i: OmegaFFI) extends Session {
  var callbacks = List.empty[ViewportCallback]

  def push(s: String): ChangeResult =
    Edit(i.omega_edit_insert(p, 0, s, 0))

  def delete(offset: Long, len: Long): ChangeResult =
    Edit(i.omega_edit_delete(p, offset, len))

  def insert(s: String, offset: Long): ChangeResult =
    Edit(i.omega_edit_insert(p, offset, s, 0))

  def overwrite(s: String, offset: Long): ChangeResult =
    Edit(i.omega_edit_overwrite(p, offset, s, 0))

  def view(offset: Long, size: Long): Viewport = {
    val vp = i.omega_edit_create_viewport(p, offset, size, null, null)
    new ViewportImpl(vp, i)
  }

  def viewCb(offset: Long, size: Long, cb: ViewportCallback): Viewport = {
    callbacks +:= cb
    val vp = i.omega_edit_create_viewport(p, offset, size, cb, null)
    new ViewportImpl(vp, i)
  }
}

object Edit {
  def apply(op: => Long): ChangeResult =
    op match {
      case 0 => ChangeFail
      case v => Changed(v)
    }
}
