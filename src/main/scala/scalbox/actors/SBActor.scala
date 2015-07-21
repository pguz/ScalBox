package scalbox.actors

import akka.actor.{Actor, ActorRef}
import scalbox.actors.protocols.SBInner


abstract class SBActor(val token: String) extends Actor {
  val selfSB: SBActorRef = new SBActorRef(self, token)
  var neighs: (SBActorRef, SBActorRef) = (selfSB, selfSB)
  var nodeMap: List[ActorRef] = List()


  def handlePlaceActor(actSB: SBActorRef) = {
    if(neighs._2 == selfSB) {
      neighs = (actSB, actSB)
      actSB.ref ! SBInner.UpdateNeighs(selfSB, selfSB)
    } else if(neighs._2.token > actSB.token || neighs._2.token == "") {
      actSB.ref ! SBInner.UpdateNeighs(selfSB, neighs._2)
      neighs._2.ref ! SBInner.UpdateLeftNeigh(actSB)
      neighs = neighs.copy(_2 = actSB)
    } else {
      neighs._2.ref ! SBInner.PlaceActor(actSB)}}



}
case class SBActorRef(val ref: ActorRef, val token: String) {
}