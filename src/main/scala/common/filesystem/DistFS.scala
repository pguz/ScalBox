package common.filesystem

import java.io.{File, FileWriter}

import common.LoggerAspect
import org.apache.commons.io.FileUtils

class DistFS(rootpath: String, val configFS: String)
  extends FileSystem(rootpath) {
  private val log = LoggerAspect(classOf[DistFS])

  var history: Map[String,(Long,Byte)] = Map()

  def init() : Unit = {

    log.info("\nNode: " + rootpath + " initialization starts.")
    val rootDir = new File(rootpath)
    val filesInfoConfig = readFilesInfo()
    history = Map()

    for (file <- rootDir.listFiles()) {
      val fileName = file.getName()
      if (filesInfoConfig.contains(fileName)) {
        val fromConfig = filesInfoConfig(fileName)
        val info = FileInfo.DistInfo(file, fromConfig._1, fromConfig._2)
        files += ((info.path, info))
        history += fileName -> fromConfig
      }
    }
    filesInfoConfig.filter(p => p._2._2 == FileStatus.deleted) foreach { e =>
      history += e._1 -> filesInfoConfig(e._1)
    }
    log.debug("\nNode: " + rootpath + " files: " + files)     //verbose
    log.debug("\nNode: " + rootpath + " history: " + history) //verbose
  }

  def getExtendedFileList(path: String): Map[String, (Long, String, Long, Byte)] = {
    //currentPath = path // czy to potrzebne?

    val filesInfoConfig = readFilesInfo()
    var filesInfo       = Map[String, (Long, String, Long, Byte)]()

    val rootDir         = new File(rootpath)
    val nestLevel: Int  = path.count(_ == '%')

    //println("path: " + path + ", " + "nestLevel: " + nestLevel)

    for (file <- rootDir.listFiles()) {

      val fileName = file.getName()
      val absPath = file.getAbsolutePath

      if(fileName.count(_ == '%') == nestLevel && absPath.startsWith(path) && absPath != path) {

        if (history.contains(fileName)) {
          val fromHistory = history(fileName)
          val info = FileInfo.DistInfo(file, fromHistory._1, fromHistory._2)
          filesInfo += ((fileName, info.DistTuple))
          println("file added: " + info.path)
        } else {
          println("file: " + path + " not in config.")
        }
      }
    }
    filesInfo
  }

  def ifHistoryContains(path: String): Boolean =
    history.contains(path)

  def addFile(path: String, name: String, info:(Long, Byte)) = {
    val file = new File(path)
    val file_info = FileInfo.DistInfo(file, info._1, info._2)
    files += ((path, file_info))
    println("file_info %s, %d".format(file_info.path,file_info.size))
    history = history + (name -> info)
    writeIntoConfigCopy(name, info._1)
  }

  def deleteFile(path: String, timestamp: Long): Boolean = {
    val fileToRemove = new File(path)
    if(!fileToRemove.exists())
      return false
    FileUtils.forceDelete(fileToRemove)
    //println("forceDelete + path: " + path)
    files -= path
    writeIntoConfigDelete(fileToRemove.getName, timestamp)
    history = history + (path -> (timestamp, FileStatus.deleted))
    return true
  }

  def applyToHistory(update: Map[String, (Long, Byte)]): Unit = {
    history = history ++ update
  }

  def readFilesInfo(): Map[String, (Long, Byte)] = {
    var fileOperations = Map[String, (Long, Byte)]()
    val source = scala.io.Source.fromFile(rootpath + "/" + configFS)
    for(l1 <- source.getLines()) {
      val fields = l1.split(" ")
      fileOperations += ((fields(0), (fields(1).toLong, fields(2).charAt(0).toByte)))

    println("fileOperations: " + fileOperations)}
    fileOperations
  }

  def writeIntoConfigDelete(s: String, timestamp: Long): Unit = {
    val fw = new FileWriter(rootpath + "/" + configFS, true)
    try {
      fw.write(s + " " + timestamp + " " + FileStatus.deleted.toChar + "\n")
    }
    finally fw.close()
  }

  def writeIntoConfigCopy(s: String, timestamp: Long): Unit = {
    val fw = new FileWriter(rootpath + "/" + configFS, true)
    try {
      println("writeIntoConfigCopy starts: " + s)
      fw.write(s + " " + timestamp + " " + FileStatus.exist.toChar + "\n")
      println("writeIntoConfigCopy ends: " + s)
    }
    finally fw.close()
  }

  def getOperationInfo(path: String) :(Long, Byte) = {
    println("getOperationInfo: " + path)
    var file_info : (Long, Byte) = (0, FileStatus.exist)
    if(history.contains(path))
      file_info = history(path)
    file_info
  }
}
