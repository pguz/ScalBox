package common.filesystem

import java.io.File

/**
 * Created by galvanize on 6/21/15.
 */
trait FSHelper {
  //reduce path from last file
  def reducePath(actPath: String, finalPath: String): Option[String] = actPath == finalPath match {
    case false  => Some(actPath.take(actPath.lastIndexOf(File.separator)))
    case true   => None
  }
}
