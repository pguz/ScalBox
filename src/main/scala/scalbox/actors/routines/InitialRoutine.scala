package scalbox.actors.routines

import akka.actor._
import common.LoggerAspect

import scala.util.Random

trait InitialRoutine {
  this: Actor =>

  val log           : LoggerAspect
  def normal()      : Receive


  val random : Random = new Random
  implicit val actorSystem   : ActorSystem = context.system
  var actorList : List[ActorRef] = List()

  var counter : Int = 0
  var replicationFactor: Int = 0
  var replicated: Boolean = false
//  def initial(nodesList: List[ActorRef], factor: Int, history: Map[String,(Long,Byte)])
//  : Receive = {
//
//
//  }
//


}
