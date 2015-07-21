package scalbox.actors.protocols

import akka.actor.ActorRef
import common.filesystem.FileInfo
import scalbox.actors.SBActorRef

object SBInner {
  sealed trait SBInner
  case class Join(token: String)
    extends SBInner
  case class FindedPlace(sender: SBActorRef, neighL: SBActorRef, neighR: SBActorRef)
    extends SBInner

  case class InitGossip()
    extends SBInner
  case class Gossip()
    extends SBInner
  case class GossipAck()
    extends SBInner
  case class GossipNoAck()
    extends SBInner
  case class GossipNoNode(actRef: ActorRef)
    extends SBInner

  case class UpdateNodeMap(refs: List[ActorRef])
    extends SBInner
  case class UpdateNodeMapAck()
    extends SBInner
  case class UpdateLeftNeigh(actRef: SBActorRef)
    extends SBInner
  case class UpdateReightNeigh(actRef: SBActorRef)
    extends SBInner
  case class UpdateNeighs(actsRef: (SBActorRef, SBActorRef))
    extends SBInner
  case class PlaceActor(actRef: SBActorRef)
    extends SBInner
  case class Replication(filesInfo: Seq[FileInfo], step: Int)
    extends SBInner
  case class FileList(dir: String, filesInfo: Seq[FileInfo])
    extends SBInner
  case class DeleteFile(path: String)
    extends SBInner
  case class SetFileInfo(name:String, path: String, size:Long, info:(Long,Byte))
    extends SBInner
  case class SetPath(path: String, info:(Long,Byte))
    extends SBInner
  case class AddFile(path: String, info:(Long,Byte), token: Boolean)
    extends SBInner
  case class ClientSetFileInfo(name:String, path: String, size:Long)
    extends SBInner
  case class ClientSetPath(path: String)
    extends SBInner
  case class ClientInfo(path: String, info:(Long,Byte))
    extends SBInner
  case class SetSender(actRef: ActorRef)
    extends SBInner
  case class SetTimestamp(timestamp: Long)
    extends SBInner
  case class CanUp(path: String, actRef: ActorRef)
    extends SBInner
  case class Bind(actRef: ActorRef, server: ActorRef,ip: String, port: Int)
    extends SBInner
  case class SetRequestNumber(num: Int)
    extends SBInner
  case class StartReplication()
    extends SBInner
  case class EndReplication()
    extends SBInner
  case class ReplicateInfo(history: Map[String,(Long,Byte)])
    extends SBInner
}


