package omega.scaladsl.api

import jnr.ffi.Pointer
import jnr.ffi.annotations.Delegate
import omega.scaladsl.{lib, OmegaFFI, ViewportImpl}

trait ViewportCallback {
  @Delegate def invoke(p: Pointer, change: Pointer): Unit =
    handle(new ViewportImpl(p, lib.omega.asInstanceOf[OmegaFFI]))

  def handle(v: Viewport): Unit
}
