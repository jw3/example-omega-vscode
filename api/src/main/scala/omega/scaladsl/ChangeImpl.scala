package omega.scaladsl

import jnr.ffi.Pointer
import omega.scaladsl.api.Change

private[scaladsl] class ChangeImpl(p: Pointer, i: OmegaFFI) extends Change {
  lazy val id: Long = i.omega_change_get_serial(p)

  lazy val offset: Long = i.omega_change_get_offset(p)

  lazy val length: Long = i.omega_viewport_get_length(p)

  lazy val name: String = i.omega_change_get_kind_as_char(p) match {
    case "D" => "Delete"
    case "I" => "Insert"
    case "O" => "Overwrite"
    case _   => "Invalid"
  }
}
