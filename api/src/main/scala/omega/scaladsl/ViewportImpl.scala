package omega.scaladsl

import jnr.ffi.Pointer
import omega.scaladsl.api.Viewport

private[scaladsl] class ViewportImpl(p: Pointer, i: OmegaFFI) extends Viewport {
  def data(): String =
    i.omega_viewport_get_data(p)

  def length: Long =
    i.omega_viewport_get_length(p)

  def offset(): Long =
    i.omega_viewport_get_offset(p)

  def capacity(): Long =
    i.omega_viewport_get_capacity(p)

  def move(offset: Long): Boolean =
    update(offset, capacity())

  def resize(capacity: Long): Boolean =
    update(offset(), capacity)

  def update(offset: Long, capacity: Long): Boolean =
    i.omega_viewport_update(p, offset, capacity) == 0

  override def toString: String = data()
}
