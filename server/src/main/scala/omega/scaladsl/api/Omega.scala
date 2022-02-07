package omega.scaladsl.api

import java.nio.file.Path

trait Omega {
  def newSession(path: Option[Path] = None): Session
}
