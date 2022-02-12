package omega.scaladsl

import jnr.ffi.Pointer
import omega.scaladsl.api.Viewport

private[scaladsl] class ViewportImpl(p: Pointer, i: OmegaFFI) extends Viewport {
  def data(): String =
    i.omega_viewport_get_data(p)

  def length: Long =
    i.omega_viewport_get_length(p)

  override def toString: String = data()
}
