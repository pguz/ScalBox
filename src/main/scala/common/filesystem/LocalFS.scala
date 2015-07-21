package common.filesystem

import java.io.File

import org.apache.commons.io.FileUtils


class LocalFS(rootpath: String)
  extends FileSystem(rootpath) {

  def init() : Unit = {
    //currentPath = rootpath
    files = Map()

    val rootDir = new File(rootpath)
    println(rootpath)
    for (file <- rootDir.listFiles()) {
      val info = FileInfo.LocalInfo(file)
      files += ((info.path, info))
    }
  }

  def getFileList(path: String): List[(String,Long,String)] = {

    var actFiles = List[(String,Long,String)]()
    //currentPath = path
    println("rootpath: " + path)
    val rootDir = new File(path)

    for (file <- rootDir.listFiles()) {
      val fileInfo = FileInfo.LocalInfo(file)
      println("info.path: " + fileInfo.path)
      if(fileInfo.size > 0)
        actFiles = fileInfo.LocalTuple :: actFiles
    }
    actFiles
  }

  def addFile(path: String, info:(Long, Byte)) = {
    val file = new File(path)
    val file_info = FileInfo.LocalInfo(file)
    files += ((path, file_info))
    println("file_info %s, %d".format(file_info.path,file_info.size))
  }

  def deleteFile(path: String, timestamp: Long): Boolean = {
    val fileToRemove = new File(path)
    FileUtils.forceDelete(fileToRemove)
    files -= path
    return true
  }

}


/*def copyFile(srcPath: String, destPath: String): Boolean = {
  println("copyFile")
  val srcFile = new File(srcPath)
  val destFile = new File(destPath)
  //val srcInfo = files(srcPath)

  if(files.contains(destFile.getAbsolutePath))
    sys.error(s"Destination exists.")

  FileUtils.copyFile(srcFile, destFile)
  files += ((destPath, FileInfo(destFile)))
  return true
}*/