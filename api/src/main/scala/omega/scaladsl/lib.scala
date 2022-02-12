package omega.scaladsl

import jnr.ffi.{LibraryLoader, Pointer}
import omega.scaladsl.api.{Omega, Session, Version, ViewportCallback}

import java.nio.file.Path

object lib {
  val omega: Omega = LibraryLoader.create(classOf[OmegaFFI]).load("omega_edit")
}

private trait OmegaFFI extends Omega {
  def omega_version_major(): Int
  def omega_version_minor(): Int
  def omega_version_patch(): Int

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

  def version(): Version = Version(omega_version_major(), omega_version_minor(), omega_version_patch())
}
