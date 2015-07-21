package client.model

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import client.model.distributed.{DistApiImpl, DistFSActor}
import client.model.local.{LocalApiImpl, LocalFSActor}
import com.typesafe.config.{Config, ConfigFactory}
import common.filesystem.LocalFS

class ClientModel extends LocalApiImpl with DistApiImpl {
  import ClientModel._

  val clientSystem: ActorSystem
    = ActorSystem(systemName, ClientModel.configClient)
  override implicit val timeout: Timeout = timeoutConfig

  val pthLclStop: String  = LocalModel.pthStop
  var pthLclDef: String   = LocalModel.pthDefault

  var pthDistStop: String  = DistModel.pthStop
  var pthDistAct: String  = DistModel.pthDefault
  var numNodes: Int       = 0

  val fileSystem      = new LocalFS(LocalModel.pthDefault)
  fileSystem.init()

  override val actLocal: ActorRef = clientSystem.actorOf(
    LocalFSActor.props(fileSystem), LocalModel.actName)
  override val actDist : ActorRef = clientSystem.actorOf(
    DistFSActor.props(DistModel.RemoteDistActorCreator.createPath), DistModel.actName)
}

object ClientModel {
  val configSBox: Config  = ConfigFactory.load().getConfig("sbox")
  val configMainClient: Config  = ConfigFactory.load().getConfig("client")

  val configClient  : Config  = ConfigFactory.load(configMainClient.getString("config"))
  val systemName: String = configClient.getString("system.name")
  val timeoutConfig   : Long = configClient.getDuration("timeout", TimeUnit.MILLISECONDS)

  object LocalModel {
    val pthDefault: String  = configClient.getString("filesystem.root")
    val pthStop   : String  = configClient.getString("filesystem.stopDir")

    val actName       : String = configClient.getString("actor.localName")
  }

  object DistModel {
    val actName     : String  = configClient.getString("actor.distName")

    val pthDefault: String  = ""
    val pthStop   : String  = ""

    object RemoteDistActorCreator {
      val protocol = configSBox.getString("system.protocol")
      val systemName = configSBox.getString("system.name")
      val host = configSBox.getString("system.ip")
      val port = configSBox.getString("system.port")
      val actorName = configSBox.getString("system.gate")

      def createPath:String = {
        s"$protocol://$systemName@$host:$port/user/$actorName"
      }
    }
  }
}

