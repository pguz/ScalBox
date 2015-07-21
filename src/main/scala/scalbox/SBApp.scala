package scalbox

import akka.actor.{ActorSystem, Props}
import com.typesafe.config._
import common.LoggerAspect
import scalbox.actors.SBSeedActor

import scala.concurrent.duration._

object SBApp extends App {
  private val log = LoggerAspect(classOf[App])

  val confMetaDist: Config  = ConfigFactory.load().getConfig("sbox")
  val confFile: String      = confMetaDist.getString("config")

  val confDist: Config        = ConfigFactory.load(confFile)
	val systemName: String    	= confDist.getString("sbox.system.name")
  val timeoutConf: Long       = confDist.getDuration("timeout", MILLISECONDS)
  val actServiceName: String  = confDist.getString("sbox.system.act")
  val replicationFactor: Int  = confDist.getInt("filesystem.replicationFactor")


  val system        = ActorSystem(systemName, confDist)
  val actSeed       = system.actorOf(Props(new SBSeedActor()), name = "actSeed")

}



