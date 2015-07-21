package scalbox.actors

import akka.actor.{ActorRef, Cancellable, Props}
import common.LoggerAspect
import common.filesystem.FileInfo
import scalbox.actors.protocols.{SBInner, SBProtocol}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SBSeedActor {
  def props(): Props =
    Props(classOf[SBServiceActor])
}

class SBSeedActor extends SBActor("") {
  private val log = LoggerAspect(classOf[SBSeedActor])
  var gossipCancallable: Cancellable = null
  var nodeNieghMap: Map[SBActorRef, (SBActorRef, SBActorRef)] = Map()
  nodeNieghMap += selfSB -> (selfSB, selfSB)

  context.system.scheduler.schedule(10 seconds, 10 seconds, selfSB.ref, SBInner.InitGossip())

  def receive: Receive = listener

  def listener: Receive = {
    case SBInner.InitGossip() =>
      handleGossip()
    case SBInner.Gossip() =>
      sender() ! SBInner.GossipAck()
      log.info("\nGossip ends")
    case SBInner.GossipAck() =>
      handleGossipAck()
    case SBInner.GossipNoAck() =>
      handleGossipNoAck()
    case SBInner.GossipNoNode(actRef: ActorRef) =>
      log.info("\nNode has gone!")
      val senderSBList = nodeNieghMap.filter(p => p._1.ref == sender()).toList
      if(senderSBList.nonEmpty) {
        val senderSB = senderSBList.head._1
        val goneNode = nodeNieghMap(senderSB)._2
        val nextNode = nodeNieghMap(goneNode)._2

        nodeNieghMap -= goneNode
        nodeMap = nodeMap.filter(e => e != goneNode.ref)

        val NeighsLeft: (SBActorRef, SBActorRef) = nodeNieghMap(senderSB)
        val NeighsRight: (SBActorRef, SBActorRef) = nodeNieghMap(nextNode)
        nodeNieghMap += senderSB -> (NeighsLeft._1, nextNode)
        nodeNieghMap += nextNode -> (senderSB, NeighsRight._2)

        sender() ! SBInner.UpdateReightNeigh(nextNode)
        nextNode.ref ! SBInner.UpdateLeftNeigh(senderSB)
      }
    case SBInner.Join(token: String) =>
      log.info("\nActor " + token + " asks Seed")
      val actJoin = new SBActorRef(sender(), token)
      nodeMap = sender() :: nodeMap
      handlePlaceActor(actJoin)
      log.debug("\nSBSeedActor state: waitingForNew")
      context.become(waitingForNew())

    case SBInner.FileList(dir: String, filesInfo: Seq[FileInfo]) =>
      log.debug("\nFileList starts:: token: " + token)
      log.debug("\nneighs._2.ref:: token: " + neighs._2.token)
      neighs._2.ref ! SBInner.FileList(dir, filesInfo)

    case SBInner.DeleteFile(path: String) =>
      neighs._2.ref ! SBInner.DeleteFile(path)

    case SBProtocol.GiveNodeMap() =>
      sender ! SBProtocol.NodeMap(nodeMap)

    case SBInner.UpdateLeftNeigh(actRef: SBActorRef) =>
      log.debug("\nUpdateLeftNeigh" + " sender " + sender() + " self " + self)
      neighs = neighs.copy(_1 = actRef)


    case unknown =>
      log.warn("\nSBSeedActor State listener Unknown message: " + unknown + " sender " + sender() + " self " + self)
      log.debug("-----------------------------------------------------")

  }

  def handleGossip() = {
    if(neighs._2 != selfSB) {
      neighs._2.ref ! SBInner.Gossip()
      gossipCancallable = context.system.scheduler.scheduleOnce(
        500 milliseconds,
        self, SBInner.GossipNoAck())
    }
  }

  def handleGossipAck() = {
    gossipCancallable.cancel()
  }

  def handleGossipNoAck() = {
    self ! SBInner.GossipNoNode(neighs._2.ref)
  }

  def waitingForNew(): Receive = {
    case SBInner.FindedPlace(senderSB: SBActorRef, neighL: SBActorRef, neighR: SBActorRef) =>
      log.info("\nNew actor has joined")
      nodeNieghMap += senderSB -> (neighL, neighR)
      val NeighsLeft: (SBActorRef, SBActorRef) = nodeNieghMap(neighL)
      val NeighsRight: (SBActorRef, SBActorRef) = nodeNieghMap(neighR)
      nodeNieghMap += neighL -> (NeighsLeft._1, senderSB)
      nodeNieghMap += neighR -> (senderSB, NeighsRight._2)
      nodeMap.foreach(ref => ref ! SBInner.UpdateNodeMap(nodeMap))
      log.info("\nSBSeedActor state: updateNodeMap")
      context.become(updateNodeMap(0))

    case SBInner.UpdateLeftNeigh(actRef: SBActorRef) =>
      log.debug("\nUpdateLeftNeigh " + " sender " + sender() + " self " + self)
      neighs = neighs.copy(_1 = actRef)

    case unknown =>
      log.warn("\nSBSeedActor State waitingForNew Unknown message: " + unknown + " sender " + sender() + " self " + self)
      log.debug("\n-----------------------------------------------------")
  }

  def updateNodeMap(ackNodes: Int): Receive = {
    case SBInner.UpdateNodeMapAck() =>
      synchronized {
        if (ackNodes < nodeMap.length - 1) {
          context.become(updateNodeMap(ackNodes + 1))
          log.debug("\nCollected UpdateNodeMapAck: " + ackNodes)
        } else {
          log.info("\nCollected all UpdateNodeMapAck")
          log.info(neighs._2.token + " starts replication")
          neighs._2.ref ! SBInner.StartReplication()
          log.debug("\nSBSeedActor state: listener")
          context.become(listener)
        }
      }


    case SBInner.UpdateLeftNeigh(actRef: SBActorRef) =>
      log.debug("\nUpdateLeftNeigh " + " sender " + sender() + " self " + self)
      neighs = neighs.copy(_1 = actRef)


    case unknown =>
      log.warn("\nSBSeedActor State updateNodeMap Unknown message: " + unknown + " sender " + sender() + " self " + self)
      log.debug("-----------------------------------------------------")

  }


}
