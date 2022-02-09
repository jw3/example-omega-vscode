package omega.scaladsl

import jnr.ffi.annotations.Delegate
import jnr.ffi.{LibraryLoader, Pointer}
import omega.scaladsl.api.{Omega, Session, Viewport}

import java.nio.file.Path

object lib {
  val omega: Omega = LibraryLoader.create(classOf[OmegaFFI]).load("omega_edit")
}

private trait OmegaFFI extends Omega {
  def omega_edit_create_session(path: String, cb: Pointer, userData: Pointer): Pointer
  def omega_edit_insert(p: Pointer, offset: Long, str: String, len: Long): Long
  def omega_edit_overwrite(p: Pointer, offset: Long, str: String, len: Long): Long
  def omega_edit_delete(p: Pointer, offset: Long, len: Long): Long
  def omega_edit_create_viewport(p: Pointer, offset: Long, size: Long, cb: ViewportCallback, userData: Pointer): Pointer

  def omega_viewport_get_length(p: Pointer): Long
  def omega_viewport_get_data(p: Pointer): String

  def newSession(path: Option[Path]): Session = new SessionImpl(
    omega_edit_create_session(path.map(_.toString).orNull, null, null),
    this
  )
}

private class SessionImpl(p: Pointer, i: OmegaFFI) extends Session {
  var callbacks = List.empty[ViewportCallback]

  def push(s: String): Unit =
    i.omega_edit_insert(p, 0, s, 0)

  def delete(offset: Long, len: Long): Unit =
    i.omega_edit_delete(p, offset, len)

  def overwrite(s: String, offset: Long): Unit =
    i.omega_edit_overwrite(p, offset, s, 0)

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

private class ViewportImpl(p: Pointer, i: OmegaFFI) extends Viewport {
  def data(): String =
    i.omega_viewport_get_data(p)

  def length: Long =
    i.omega_viewport_get_length(p)

  override def toString: String = data()
}

trait ViewportCallback {
  @Delegate def invoke(p: Pointer, change: Pointer): Unit =
    handle(new ViewportImpl(p, lib.omega.asInstanceOf[OmegaFFI]))

  def handle(v: Viewport): Unit
}
