package omega.scaladsl.api

import jnr.ffi.Pointer
import jnr.ffi.annotations.Delegate
import omega.scaladsl.{lib, ChangeImpl, OmegaFFI, ViewportImpl}

trait ViewportCallback {
  @Delegate def invoke(p: Pointer, c: Pointer): Unit = {
    val i = lib.omega.asInstanceOf[OmegaFFI]
    val change = c.address() match {
      case 0 | 1 | 2 => None
      case _         => Some(new ChangeImpl(c, i))
    }
    handle(new ViewportImpl(p, i), change)
  }

  def handle(v: Viewport, change: Option[Change]): Unit
}
