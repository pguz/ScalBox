package common.filesystem

//przemyslec nad funkcjonalnoscia, czy ma zawierac stan czy nie
abstract class FileSystem(val rootpath: String) {
  var files = Map[String, FileInfo]()

  def ifContains(path: String): Boolean =
    files.contains(path)

  def getFileSize(path: String): Option[Long] = {
    files.get(path).map(elem => elem.size)
  }
}
