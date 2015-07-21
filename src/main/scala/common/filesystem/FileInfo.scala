package common.filesystem

import java.io.File
import java.util.Date

object FileStatus {
  val deleted : Byte  = 'D'
  val exist   : Byte  = 'E'
  val unknown : Byte  = 'U'
}

case class FileInfo(
             name   : String,
             path   : String,
             size   : Long,
             date   : String,
             parent : String,
             id     : Long,
             status : Byte) {

  def dispArray: Array[AnyRef]
    =  Array[AnyRef](name, size.toString, date)

  def LocalTuple: (String, Long, String)
    = (name, size, date)

  def DistTuple: (Long, String, Long, Byte)
    = (size, date, id, status)
}

object FileInfo {

  def LocalInfo(f: File): FileInfo = {
    new FileInfo(f.getName, f.getAbsolutePath, f.length(), (new Date(f.lastModified)).toString, f.getParent, 0, 'U')
  }

  def DistInfo(f: File, ts: Long, status: Byte): FileInfo = {
    val fileName: String    = parseName(f.getName)
    val parentPath: String  = f.getParent
    new FileInfo(fileName, parentPath + '/' + fileName, f.length(), (new Date(f.lastModified)).toString, parentPath, ts, status)
  }

  private def parseName(s: String): String = {
    val ss: Array[String] = s.split("%")
    ss(ss.length - 1)
  }
}