package client.model.local

import akka.actor.{Actor, Props}
import akka.util.Timeout
import common.LoggerAspect
import common.filesystem.LocalFS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object LocalFSActor {
  def props(fs: LocalFS)(implicit timeout: Timeout): Props =
    Props(classOf[LocalFSActor], fs, timeout)
}

class LocalFSActor(val fs: LocalFS)(implicit val timeout: Timeout) extends Actor {

  private val log = LoggerAspect(classOf[LocalFSActor])

  override def receive: Receive = {
    case FSLocalProtocol.GetFileList(dir) =>
      handleGetFileList(dir)
    case FSLocalProtocol.DeleteFile(path)  =>
      handleDeleteFile(path)
    case unknown =>
      log.warn("Unknown message: " + unknown)
  }

  def handleGetFileList(dir: String): Unit = {
    log.debug("GetFileList starts:: dir: " + dir)
    val files :List[(String,Long,String)] = fs.getFileList(dir)
    sender ! FSLocalProtocol.Files(files)
  }

  def handleDeleteFile(path: String): Unit = {
    log.debug("DeleteFile starts: " + path)
    Future {
      Try(fs.deleteFile(path, System.currentTimeMillis()))
      fs.files
    }
    sender ! FSLocalProtocol.Deleted()
  }
}
