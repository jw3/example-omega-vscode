package omega.scaladsl

import jnr.ffi.Pointer
import omega.scaladsl.api.Change

private[scaladsl] class ChangeImpl(p: Pointer, i: OmegaFFI) extends Change {
  lazy val id: Long = i.omega_change_get_serial(p)

  lazy val offset: Long = i.omega_change_get_offset(p)

  lazy val length: Long = i.omega_viewport_get_length(p)

  def data(): Array[Byte] = i.omega_change_get_bytes(p).getBytes()

  lazy val operation: Change.Op = i.omega_change_get_kind_as_char(p) match {
    case "D" => Change.Delete
    case "I" => Change.Insert
    case "O" => Change.Overwrite
    case _   => Change.Undefined
  }
}
