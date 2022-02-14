package omega.scaladsl.api

trait Change {
  def id(): Long
  def offset(): Long
  def length(): Long
}

object Change {
  sealed trait Result
  case object Fail extends Result
  case class Changed(id: Long) extends Result
}
