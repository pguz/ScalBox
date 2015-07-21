package client.model.distributed.routines

import akka.actor.{Actor, ActorRef}
import client.model.distributed.{DistFSActor, FSDistProtocol}
import common.LoggerAspect
import scalbox.actors.protocols.SBProtocol

trait DeletingRoutine {
  this: DistFSActor =>

  val log           : LoggerAspect
  var requestSender : ActorRef
  def connected()   : Receive
  var counter       : Int = 0

  def deleting(reqSender : ActorRef, numRequest: Int): Receive = {
    case SBProtocol.Deleted() => handleMessage(reqSender, numRequest)
    case SBProtocol.NotContain() => handleMessage(reqSender, numRequest)
    case unknown =>
      log.debug("State DELETING Unknown message" + unknown)
      context.become(connected())
  }

  def handleMessage(reqSender : ActorRef, numRequest: Int): Unit = {
    counter += 1
    if(counter == numRequest) {
      context.become(connected())
      reqSender ! FSDistProtocol.Deleted()
      counter = 0
    }
  }

}
