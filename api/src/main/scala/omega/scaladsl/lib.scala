package omega.scaladsl

import jnr.ffi.{LibraryLoader, Pointer}
import omega.scaladsl.api.{Omega, Session, SessionCallback, Version, ViewportCallback}

import java.nio.file.Path

object lib {
  val omega: Omega = LibraryLoader.create(classOf[OmegaFFI]).failImmediately().load("omega_edit")

  // HACK;; prevent callbacks from being garbage collected
  private[scaladsl] var callbacks = List.empty[AnyRef]
}

private trait OmegaFFI extends Omega {
  def omega_version_major(): Int
  def omega_version_minor(): Int
  def omega_version_patch(): Int

  def omega_edit_create_session(path: String, cb: SessionCallback, userData: Pointer): Pointer
  def omega_edit_insert(p: Pointer, offset: Long, str: String, len: Long): Long
  def omega_edit_overwrite(p: Pointer, offset: Long, str: String, len: Long): Long
  def omega_edit_delete(p: Pointer, offset: Long, len: Long): Long
  def omega_edit_create_viewport(p: Pointer, offset: Long, size: Long, cb: ViewportCallback, userData: Pointer): Pointer

  def omega_viewport_get_length(p: Pointer): Long
  def omega_viewport_get_data(p: Pointer): String

  def omega_change_get_serial(p: Pointer): Long
  def omega_change_get_offset(p: Pointer): Long
  def omega_change_get_length(p: Pointer): Long
  def omega_change_get_kind_as_char(p: Pointer): String
  def omega_session_get_change(p: Pointer): Pointer

  def newSession(path: Option[Path]): Session = new SessionImpl(
    omega_edit_create_session(path.map(_.toString).orNull, null, null),
    this
  )

  def newSessionCb(path: Option[Path], cb: SessionCallback): Session = {
    lib.callbacks +:= cb
    new SessionImpl(
      omega_edit_create_session(path.map(_.toString).orNull, cb, null),
      this
    )
  }

  def version(): Version = Version(omega_version_major(), omega_version_minor(), omega_version_patch())
}
