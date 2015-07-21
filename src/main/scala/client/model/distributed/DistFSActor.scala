package client.model.distributed

import akka.actor._
import akka.util.Timeout
import client.model.distributed.routines._
import common.LoggerAspect
import scalbox.actors.protocols.SBProtocol

import scala.util.Random

object DistFSActor {
  def props(actorDistPath: String)(implicit timeout: Timeout): Props = Props(classOf[DistFSActor], actorDistPath, timeout)
}

class DistFSActor(actorDistPath: String)(implicit val timeout: Timeout)
  extends Actor with ConnectRoutine with GettingFSRoutine with GettingRoutine with SendingRoutine with DeletingRoutine {

  override val log           = LoggerAspect(classOf[DistFSActor])
  override implicit val actorSystem   : ActorSystem = context.system
  override var serverGate    = context.actorSelection(actorDistPath)

  override def receive  : Receive = disconnected()

  override def connected(): Receive = {
    case FSDistProtocol.GetFileList(path: String, numNodes: Int) =>
      handleGetFS(path, numNodes)
    case FSDistProtocol.SendFile(srcInfo: (String, String, Long), dstPath: String, numNodes: Int) =>
      handleSendFile(srcInfo, dstPath, numNodes)
    case FSDistProtocol.GetFile(srcPath: String, dstPath: String) =>
      handleGetFile(srcPath, dstPath)
    case FSDistProtocol.DeleteFile(path: String, numNodes: Int) =>
      handleDeleteFile(path, numNodes)
    case unknown =>
      log.warn("Connected Unknown message: " + unknown)
  }

  def handleGetFS(path: String, numNodes: Int): Unit = {
    log.debug("GetFilesList starts:: nodeMap " + nodeMap)
    log.debug("State: GETTING FS")

    context.become(gettingFS(sender(), numNodes))
    var countChosen: Int = 0
    val random : Random = new Random
    var randNodes: List[ActorRef] = List()

    while (countChosen < numNodes) {
      var actor: ActorRef = null
      do {
        actor = nodeMap(random.nextInt(nodeMap.length))
      } while (randNodes.contains(actor))
      countChosen += 1
      randNodes = actor :: randNodes
    }
    println("randNodes.len: " + randNodes.length)
    randNodes.foreach((actor: ActorRef) => actor ! SBProtocol.GetFileList(path))
  }

  def handleGetFile(srcPath: String, dstPath: String): Unit = {
    log.debug("GetFile starts:: dirPath: " + srcPath + " dstPath:" + dstPath)
    log.debug("State: GETTING")

    context.become(getting(sender(), srcPath, dstPath,nodeMap.size))
    nodeMap.foreach((actor: ActorRef) => actor ! SBProtocol.CanUpload(dstPath))
  }

  def handleSendFile(srcInfo: (String, String, Long), dstPath: String, numNodes: Int): Unit = {
    log.debug("SendFile starts:: name: " + srcInfo._1 + " srcPath:" + srcInfo._2 + ", dstPath: " + dstPath)
    log.debug("State: SENDING")

    context.become(sending(sender(), srcInfo._1, srcInfo._2, srcInfo._3, numNodes))
    var countChosen: Int = 0
    val random : Random = new Random
    var randNodes: List[ActorRef] = List()

    while (countChosen < numNodes) {
      var actor: ActorRef = null
      do {
        actor = nodeMap(random.nextInt(nodeMap.length))
      } while (randNodes.contains(actor))
      countChosen += 1
      randNodes = actor :: randNodes
    }
    randNodes.foreach((actor: ActorRef) => actor ! SBProtocol.CanDownload(srcInfo._1, dstPath))

  }

  def handleDeleteFile(path: String, numNodes: Int): Unit = {
    log.debug("DeleteFile starts:: path: " + path + ", numNodes: " + numNodes)

    context.become(deleting(sender(), numNodes))

    var countChosen: Int = 0
    val random : Random = new Random
    var randNodes: List[ActorRef] = List()

    while (countChosen < numNodes) {
      var actor: ActorRef = null
      do {
        actor = nodeMap(random.nextInt(nodeMap.length))
      } while (randNodes.contains(actor))
      countChosen += 1
      randNodes = actor :: randNodes
    }

    randNodes.foreach((actor: ActorRef) => actor ! SBProtocol.DeleteFile(path, System.currentTimeMillis()))
  }
}
