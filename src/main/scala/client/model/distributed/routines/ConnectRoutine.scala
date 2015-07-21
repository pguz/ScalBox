package client.model.distributed.routines

import akka.actor.{Actor, ActorRef, ActorSelection}
import client.model.distributed.{DistFSActor, FSDistProtocol}
import common.LoggerAspect
import scalbox.actors.protocols.SBProtocol

trait ConnectRoutine {
  this: Actor =>

  val log           : LoggerAspect  = null
  var requestSender : ActorRef      = null
  def connected()   : Receive       = null

  var serverGate    : ActorSelection
  var nodeMap       : List[ActorRef]      = null

  def disconnected(): Receive = {
    case FSDistProtocol.Connect() =>
      requestSender = sender()
      handleConnect()
    case SBProtocol.Shake()   =>
      sender ! SBProtocol.GiveNodeMap()
    case SBProtocol.NodeMap(nMap) =>
      handleNodeMap(nMap)
    case unknown =>
      log.warn("Disconnected unknown message: " + unknown)
  }

  def handleConnect(): Unit = {
    log.debug("Start connecting: " + serverGate.toString())
    requestSender = sender()

    serverGate ! SBProtocol.Hand()
  }

  def handleNodeMap(nMap: List[ActorRef]): Unit = {
    log.debug("Got node map " + nMap)

    nodeMap = nMap
    requestSender ! FSDistProtocol.Connected(nodeMap.length)
    context.become(connected())
  }

}
