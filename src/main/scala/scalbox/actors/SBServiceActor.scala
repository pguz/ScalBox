package scalbox.actors

import java.net.InetSocketAddress

import akka.actor._
import client.model.distributed.handlers.ClientDownActor
import common.LoggerAspect
import common.filesystem.FileStatus
import scalbox.actors.handlers.{SBDownActor, SBUpActor}
import scalbox.actors.protocols.{SBInner, SBProtocol}
import scalbox.actors.routines.ReplicationRoutine
import scalbox.model.Node

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

object SBServiceActor {
  def props(actSeed: ActorSelection, replicationFactor: Int, token: String, node: Node): Props =
    Props(classOf[SBServiceActor], actSeed, replicationFactor, token, node)
}

class SBServiceActor(actSeed: ActorSelection, replicationFactor: Int, token: String, node: Node) extends SBActor(token)
  with Actor with ReplicationRoutine {

  val log = LoggerAspect(classOf[SBServiceActor])

  implicit val actSystem = context.system

  var actSender : List[(ActorRef, Int)] = List()
  val senderNum : Int = 7
  var i : Int = 0
  for (i <- 0 until senderNum) {
    val port = node.portUp + i
    log.debug("\nActor for " + port + " is created")

    actSender =
      (context.actorOf(Props(new SBUpActor(new InetSocketAddress(node.ip, node.portUp + i))), node.name + "UP" + i),port) :: actSender
  }

  actSender = actSender.reverse

  var senderIndex : Int = 0

  val actDown = context.actorOf(Props(new SBDownActor(node, new InetSocketAddress(node.ip, node.portDown))), node.name + "DOWN")
  override val actDownload = context.actorOf(Props(new ClientDownActor()), node.name + "REPLICATOR")

  val random : Random = new Random
  var replicated: Boolean = false
  var actors : List[ActorRef] = null
  var replicationCounter : Int = 0
  var gossipCancallable: Cancellable = null

  actSeed ! SBInner.Join(token)

  def receive: Receive = findingPlace

  def findingPlace: Receive = {
    case SBInner.UpdateNeighs(actsRef: (SBActorRef, SBActorRef)) =>
      log.debug("\nSBServiceActor " + token + " has found place: " + "(" + actsRef._1.token + ", " + actsRef._2.token + ")")
      neighs = neighs.copy(_1 = actsRef._1, _2 = actsRef._2)
      actSeed ! SBInner.FindedPlace(selfSB, actsRef._1, actsRef._2)
      log.info("\nSBServiceActor " + token + " state: normal")
      context.become(normal())

    case unknown =>
      log.warn("\nState finingPlace Unknown message: " + unknown)
  }

  def normal(): Receive = {
    case SBInner.Gossip() =>
      handleGossip()
    case SBInner.GossipAck() =>
      handleGossipAck()
    case SBInner.GossipNoAck() =>
      handleGossipNoAck()

    case SBInner.UpdateNodeMap(refs: List[ActorRef]) =>
      sender() ! SBInner.UpdateNodeMapAck()
      log.debug("\nUpdateNodeMap" + " sender " + sender() + " self " + self)
      actors = refs.filter(p => p != self)
      replicated = false

    case SBInner.PlaceActor(actSBRef: SBActorRef) =>
      log.trace("\nPlaceActor: " + actSBRef.token + " sender " + sender() + " self " + self)
      handlePlaceActor(actSBRef)

    case SBInner.StartReplication() =>
      handleStartReplication()

    case SBInner.ReplicateInfo(replicateHistory) =>
      log.debug("\nReplicateInfo" + " sender " + sender() + " self " + self)
      handleReplicateInfo(replicateHistory)

    case SBInner.UpdateLeftNeigh(actRef: SBActorRef) =>
      log.debug("\nUpdateLeftNeigh " + " sender " + sender() + " self " + self)
      neighs = neighs.copy(_1 = actRef)

    case SBInner.UpdateReightNeigh(actRef: SBActorRef) =>
      log.debug("\nUpdateReightNeigh " + " sender " + sender() + " self " + self)
      neighs = neighs.copy(_2 = actRef)

    case SBProtocol.GiveNodeMap() =>
      actSeed forward SBProtocol.GiveNodeMap()

    case SBProtocol.Hand() =>
      handleHand()

    case SBProtocol.GetFileList(dir: String) =>
      handleGetFileList(dir)

    case SBProtocol.CanDownload(srcFileName: String, dstPath: String) =>
      handleCanDownload(srcFileName, dstPath)

    case SBProtocol.CanUpload(dstPath: String) =>
      handleCanUpload(dstPath)

    case SBProtocol.DeleteFile(path: String, timestamp: Long) =>
      handleDeleteFile(path, timestamp)

    case SBInner.AddFile(path, info, token) =>
      handleAddFile(path, info, token)

    case SBInner.EndReplication() =>
      log.debug("\ncase SBInner.EndReplication()")
      handleEndReplication()

    case unknown => {
      log.warn("\nState normal Unknown message: " + unknown)
    }
  }

  def handleGossip() = {
    neighs._2.ref ! SBInner.Gossip()
    sender() ! SBInner.GossipAck()
    gossipCancallable = context.system.scheduler.scheduleOnce(
      500 milliseconds,
      self, SBInner.GossipNoAck())
  }

  def handleGossipAck() = {
    gossipCancallable.cancel()
  }

  def handleGossipNoAck() = {
    actSeed ! SBInner.GossipNoNode(neighs._2.ref)
  }

  def handleHand(): Unit = {
    log.debug("\nHand Received")
    sender() ! SBProtocol.Shake()
  }

  def handleGetFileList(dir: String): Unit = {
    log.debug("\nGetFileList starts:: dir:%s\n".format(dir))
    val files = node.fs.getExtendedFileList(returnAbsolutePath(dir + '/'))
    println("files.size: " + files.size)
    sender ! SBProtocol.ExtendedFiles(files)
  }

  def handleCanDownload(srcFileName: String, dstPath: String): Unit = {
    log.debug("\nCanDown starts:: from: " + srcFileName)

    val dir = returnAbsolutePath("/")
    val file_path = dir + srcFileName
    val file_info: (Long, Byte) = node.fs.getOperationInfo(file_path)

    actDown ! SBInner.SetPath(dir, file_info)
    sender ! SBProtocol.SendInfo(actDown, node.ip, node.portDown)
    log.debug("\nCanDown ends ")
  }

  def handleCanUpload(path: String): Unit = {

    val absolutePath = returnAbsolutePath(path)
    log.debug("\nDownload starts:: path: " + absolutePath)

    if (node.fs.ifContains(absolutePath)) {
      println("contains")
      val senderActor = getSender()
      senderActor._1 ! SBInner.SetFileInfo(path.split("/").last,
        absolutePath, node.fs.getFileSize(absolutePath).get, node.fs.getOperationInfo(path.split("/").last))
      log.debugDescAsp("\nhandleCanUp ends:: ", sender ! SBProtocol.SendInfo(senderActor._1, node.ip, senderActor._2))
    } else {
      log.debugDescAsp("\nhandleCanUp ends:: ", sender ! SBProtocol.NotContain())
    }
  }

  def handleDeleteFile(path: String, timestamp: Long) = {
    log.debug("\nDeleteFile starts:: path: " + path)
    val normPath: String = returnAbsolutePath(path)
    log.debug("\nDeleteFile starts:: normPath: " + normPath)
    println("node.fs.files2: " + node.fs.files)
    if (node.fs.ifContains(normPath)) {
      log.debug("\nContain")
      node.fs.deleteFile(normPath, timestamp)
      sender() ! SBProtocol.Deleted()
    } else {
      log.debug("\nNot contain")
      sender() ! SBProtocol.NotContain()
    }
  }

  def handleAddFile(name: String, info: (Long, Byte), token: Boolean): Unit = {
    log.debug("Add to history " + name + " timestamp " + info._1 + " status " + info._2.toChar + " token " + token)
    node.fs.addFile(returnAbsolutePath(name), name, info)
    if (token) {
      println("node.fs.files1: " + node.fs.files)
      var smallHistory: Map[String, (Long, Byte)] = Map()
      smallHistory += name -> info
      Thread.sleep(500)
      replicate(smallHistory)
    }
  }

  def returnRelativePath(relativePath: String): String
  = {
    println("relativePath: " + relativePath)
    var offset: Int = 0
    if (relativePath.length > 0 && relativePath.charAt(0) == '/')
      offset = 1
    val relPath = relativePath.substring(offset)
    relPath.replace("/", "%")
  }


  def returnAbsolutePath(relativePath: String): String
  = {
    var offset: Int = 0
    if (relativePath.length > 0 && relativePath.charAt(0) == '/')
      offset = 1
    val relPath = relativePath.substring(offset)
    node.fs.rootpath + "/" + relPath.replace("/", "%")
  }

  def handleStartReplication() {
    log.debug("\nSBServiceActor " + token + " is starting replication")
    if (!replicated) {
      replicate(node.fs.history)
    }
  }

  def replicate(history: Map[String, (Long, Byte)]): Unit = {
    var replicators: List[ActorRef] = List()

    var countChosen: Int = 0
    var factor: Int = replicationFactor

    if (factor > actors.length)
      factor = actors.length

    log.debug("\nModified replication factor " + factor)

    while ((factor != 0) && (countChosen < factor)) {
      var actor: ActorRef = null
      do {
        actor = actors(random.nextInt(actors.length))
      } while (replicators.contains(actor))
      countChosen += 1
      replicators = actor :: replicators
    }

    log.debug("\nSending ReplicationInfo to random actors")
    replicators.foreach(actor => actor ! SBInner.ReplicateInfo(history))
  }

  def handleReplicateInfo(replicateHistory: Map[String, (Long, Byte)]): Unit = {
    log.debug("\nSBServiceActor + " + token + " starts replication " + replicateHistory)

    val byte: Byte = FileStatus.deleted
    val long: Long = 0
    val updateHistory: Map[String, (Long, Byte)] = replicateHistory.map {
      case (k, v) =>
        k -> {
          if (node.fs.history.getOrElse(k, (long, byte))._1 < v._1) v else (long, byte)
        }
    }
    replicateDeleted(updateHistory)
    replicateExisitng(updateHistory)
  }

  def replicateDeleted(updateHistory: Map[String, (Long, Byte)]): Unit = {
    val update = updateHistory.filter(p => p._2._1 > 0 && p._2._2 == FileStatus.deleted)
    if (update.nonEmpty) {
      log.debug("\nReplication deleted files " + update)
      update.foreach { m =>
        val absPath: String = returnAbsolutePath(m._1)
        if(node.fs.deleteFile(absPath, m._2._1)) {
          log.debug("\nFile " + m._1 + " was deleted from " + token)
        } else {
          log.debug("\nFile " + m._1 + " was not deleted from " + token)
        }
      }
    }
  }

  def replicateExisitng(updateHistory: Map[String, (Long, Byte)]) {
    val update = updateHistory.filter(p => p._2._1 > 0 && p._2._2 == FileStatus.exist).toList

    if (update.nonEmpty) {
      log.debug("\nReplication existing files " + update)
      sender ! SBProtocol.CanUpload("/" + update.head._1)
      context.become(getting(sender(), update, update.head, node.fs.rootpath))
    } else {
      sender ! SBInner.EndReplication()
    }
  }

  def handleEndReplication(): Unit = {
    log.debug("\nEndReplication " + sender() )
    if(!replicated) {
      replicationCounter += 1
      if (replicationCounter == actors.length) {
        replicated = true
        neighs._2.ref ! SBInner.StartReplication()
        replicationCounter = 0
      }
    }

  }

  def getSender() : (ActorRef, Int) = {
    val actor = actSender(senderIndex)

    if(senderIndex + 1 < actSender.size) {
      senderIndex += 1
    } else {
      senderIndex = 0
    }
    actor
  }

}
