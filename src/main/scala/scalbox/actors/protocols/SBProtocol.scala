package scalbox.actors.protocols

import akka.actor.ActorRef
import akka.io.Tcp
import common.filesystem.FileInfo

object SBProtocol {
  sealed trait SBRequest
  case class Hand()
    extends SBRequest

  case class Token()
    extends SBRequest

  case class GiveNodeMap()
    extends SBRequest

  case class NodeMap(nodeMap: List[ActorRef])
    extends SBResponse

  case class GetFileList(dir: String)
    extends SBRequest

  case class CanDownload(srcFileName: String, destPath:String)
    extends SBRequest

  case class GetFile(path: String)
    extends SBRequest

  case class CanUpload(path: String)
    extends SBRequest

  case class SendFile(src: String, dest: String)
    extends SBRequest

  case class DeleteFile(path: String, timestamp: Long)
    extends SBRequest

  sealed trait SBResponse
  case class Shake()
    extends SBRequest

  case class Files(filesInfo: List[FileInfo])
    extends SBResponse

  case class ExtendedFiles(files: Map[String, (Long,String, Long,Byte)])
    extends SBResponse

  case class GetInfo(actTcp: ActorRef)
    extends SBResponse

  case class SendInfo(actTcp: ActorRef, ip: String, port: Int)
    extends SBResponse

  case class NotContain()
    extends SBResponse

  case class Deleted()
    extends SBResponse

  case class Ack()
    extends Tcp.Event

  case class Timestamp()
    extends Tcp.Event
}

