package omega.scaladsl.api

import java.nio.file.Path

trait Omega {
  def version(): Version
  def newSession(path: Option[Path]): Session
  def newSessionCb(path: Option[Path], cb: SessionCallback): Session
}
