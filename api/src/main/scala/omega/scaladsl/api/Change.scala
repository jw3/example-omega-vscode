package omega.scaladsl.api

trait Change {
  def id(): Long
  def offset(): Long
  def length(): Long
}

trait ChangeResult
case object ChangeFail extends ChangeResult
case class Changed(id: Long) extends ChangeResult
