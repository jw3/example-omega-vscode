package omega.scaladsl.api

import jnr.ffi.Pointer
import jnr.ffi.annotations.Delegate
import omega.scaladsl.{lib, ChangeImpl, OmegaFFI, ViewportImpl}

trait ViewportCallback {
  @Delegate def invoke(p: Pointer, c: Pointer): Unit = {
    val i = lib.omega.asInstanceOf[OmegaFFI]
    handle(new ViewportImpl(p, i), new ChangeImpl(c, i))
  }

  def handle(v: Viewport, change: Change): Unit
}
