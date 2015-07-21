package client.model.distributed

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import common.LoggerAspect

import scala.concurrent.Future

trait DistApi {
  def fConnect: Future[Int]
  def fDisconnect: Future[Boolean]
  def fGetFiles(path: String, numNodes: Int): Future[List[(String,Long,String)]]
  def fDeleteFile(path: String, numNodes: Int): Future[Boolean]
  def fCopyFile(srcFile: (String, String, Long), dstPath: String, numNodes: Int): Future[Boolean]
  def fGetFile(srcPath: String, dstPath: String): Future[Boolean]
}

trait DistApiImpl extends DistApi {
  import FSDistProtocol._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val log = LoggerAspect(classOf[DistApiImpl])
  def actDist(): ActorRef
  implicit def timeout: Timeout

  override def fConnect: Future[Int] = {
    log.debug("Future Connect starts")
    val f = actDist ? Connect()
    f.mapTo[FSDistResponse].map{
      case Connected(numNodes: Int) => log.debugAsp("Future Connect ends:: numNodes: " + numNodes, numNodes)
      case Disconnected() => 0
      case _ => 0
    }
  }

  override def fDisconnect: Future[Boolean] = {
    val f = actDist ? Disconnect()
    f.mapTo[Disconnected].map{_ => true}
  }

  override def fGetFiles(path: String, numNodes: Int): Future[List[(String,Long,String)]] = {
    log.debug("Future GetFile starts:: path: " + path)
    val f = actDist ? GetFileList(path, numNodes: Int)
    f.mapTo[Files].map {
      case Files(files) => log.debugAsp("Future GetFile ends:: path: " + path, files)
    }
  }


  override def fDeleteFile(path: String, numNodes: Int): Future[Boolean] = {
    log.debug("Future DeleteFile starts:: path: " + path)
    val f = actDist ? DeleteFile(path, numNodes)
    f.mapTo[Deleted].map(_ => true)
  }

  override def fCopyFile(srcFile: (String, String, Long), dstPath: String, numNodes: Int): Future[Boolean] = {
    log.debug("Future CopyFile starts:: from: " + srcFile._2 + " to: " + dstPath)
    val f = actDist ? SendFile(srcFile, dstPath, numNodes)
    f.mapTo[CompletedUpload].map(_ => true)
  }

  override def fGetFile(srcPath: String, dstPath: String): Future[Boolean] = {
    log.debug("Future GetFile starts:: dirPath: " + srcPath + " dstPath: " + dstPath)
    val f = actDist ? GetFile(srcPath, dstPath)
    f.mapTo[CompletedUpload].map(_ => true)
  }
}