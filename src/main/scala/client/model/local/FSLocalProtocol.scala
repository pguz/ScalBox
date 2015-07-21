package client.model.local

object FSLocalProtocol {
  sealed trait FSLocalRequest
  case class GetFileList(path: String)
    extends FSLocalRequest
  case class DeleteFile(path: String)
    extends FSLocalRequest

  sealed trait FSLocalResponse
  case class Files(filesInfo: List[(String,Long,String)])
    extends FSLocalResponse
  case class Deleted()
    extends FSLocalResponse
}


//private val log = LoggerAspect(classOf[FSFileManager])

//def clientSystem    : ActorSystem
//implicit def timeout: Timeout
//def fGetFile(srcPath: String, dstPath: String)(implicit act: ActorRef): Future[Boolean] = {
//log.debug("Future GetFile starts:: dirPath: " + srcPath + " dstPath: " + dstPath)
//val f = act ? FSProtocol.GetFile(srcPath, dstPath)
//f.mapTo[FSProtocol.CompletedUpload].map(_ => true)
//}