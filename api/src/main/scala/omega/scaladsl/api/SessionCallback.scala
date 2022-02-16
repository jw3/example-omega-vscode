package omega.scaladsl.api

import jnr.ffi.Pointer
import jnr.ffi.annotations.Delegate
import omega.scaladsl.{lib, OmegaFFI, SessionImpl}

trait SessionCallback {
  @Delegate def invoke(p: Pointer, e: Pointer, c: Pointer): Unit =
    handle(new SessionImpl(p, lib.omega.asInstanceOf[OmegaFFI]))

  def handle(v: Session): Unit
}

object SessionCallback {
  def apply(cb: (Session) => Unit): SessionCallback =
    (v: Session) => cb(v)
}
