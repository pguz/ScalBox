package client.model.local

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import common.LoggerAspect

import scala.concurrent.Future

trait LocalApi {
  def fGetFiles(path: String): Future[List[(String,Long,String)]]
  def fDeleteFile(path: String): Future[Boolean]
}

trait LocalApiImpl extends LocalApi {
  import scala.concurrent.ExecutionContext.Implicits.global
  import FSLocalProtocol._

  private val log = LoggerAspect(classOf[LocalApiImpl])
  def actLocal(): ActorRef
  implicit def timeout: Timeout

  override def fGetFiles(path: String): Future[List[(String,Long,String)]] = {
    log.debug("Future GetFile starts:: path: " + path)
    val f = actLocal ? GetFileList(path)
    f.mapTo[Files].map {
      case Files(files) => log.debugAsp("Future GetFile ends:: path: " + path, files)
    }
  }

  override def fDeleteFile(path: String): Future[Boolean] = {
    log.debug("Future DeleteFile starts:: path: " + path)
    val f = actLocal ? DeleteFile(path)
    f.mapTo[Deleted].map(_ => true)
  }
}