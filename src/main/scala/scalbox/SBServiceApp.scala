package scalbox

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import scalbox.actors.SBServiceActor
import scalbox.model.Node

object SBServiceApp extends App {

  if(!ConfigValidator.argLength(args))
    System.exit(0)

  val nodeId = ConfigValidator.toNodeId(args(0))

  val confDist: Config        = ConfigFactory.load("sbserviceactor")
  //val timeoutConf: Long       = confDist.getDuration("timeout", MILLISECONDS)

  val replicationFactor: Int  = confDist.getInt("filesystem.replicationFactor")
  val systemName: String      = confDist.getString("filesystem.nodeSystem") + nodeId
  val actorName: String      = confDist.getString("filesystem.nodeActor") + nodeId
  val confFS: String          = confDist.getString("filesystem.configFS")

  val comConfPath: String     = "filesystem.node" + nodeId
  val actHandlerName: String  = confDist.getString("filesystem.nodeHandler") + nodeId
  val ipHandle: String        = confDist.getString(comConfPath + ".address.ip")
  val portHandle: Int = confDist.getInt(comConfPath + ".address.port")
  val portDown: Int           = confDist.getInt(comConfPath + ".address.portDown")
  val portUp: Int             = confDist.getInt(comConfPath + ".address.portUp")
  val pathDefFS: String       = confDist.getString(comConfPath + ".dir.root")
  val pathStopFS: String      = confDist.getString(comConfPath + ".dir.stop")
  val node = Node(actHandlerName, ipHandle, portDown, portUp, confFS, pathDefFS, pathStopFS)

  val system        = ActorSystem(systemName, confDist.withValue("akka.remote.netty.tcp.port",
    ConfigValueFactory.fromAnyRef(6000 + nodeId * 10)))

  val pathSeed : String = "akka.tcp://%s@%s:%s/user/%s".format(
    confDist.getString("sbox.name"),
    confDist.getString("sbox.seed.ip"),
    confDist.getString("sbox.seed.port"),
    confDist.getString("sbox.seed.name"))

  val actSeed = system.actorSelection(pathSeed)

  val actor_name: String = node.fs.rootpath
  system.actorOf(SBServiceActor.props(actSeed, replicationFactor, actor_name, node), name = "SBHandler" + nodeId)

}

object ConfigValidator {
  val usage = """
    Usage: node_id [Int]
              """

  def argLength(args: Array[String]): Boolean = {
    if (args.length == 0) {
      println(usage)
      return false
    }
    return true
  }

  def toNodeId(s: String): Int = {
    var id = -1
    try {
      id = s.toInt
    } catch {
      case e: Exception =>
        println(usage)
        return -1
    }
    return id
  }
}