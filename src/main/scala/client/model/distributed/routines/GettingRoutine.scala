package client.model.distributed.routines

import akka.actor._
import akka.io.Tcp
import client.model.distributed.{DistFSActor, FSDistProtocol}
import client.model.distributed.handlers.ClientDownActor
import com.typesafe.config.{Config, ConfigFactory}
import common.LoggerAspect
import scalbox.actors.protocols.{SBInner, SBProtocol}

trait GettingRoutine {
  this: DistFSActor =>

  val log           : LoggerAspect
  var requestSender : ActorRef
  def connected()   : Receive

  private val configMainClient : Config  = ConfigFactory.load().getConfig("client")
  private val config           : Config  = ConfigFactory.load(configMainClient.getString("config"))

  val gettingName   : String = config.getString("system.gettingName")

  implicit val actorSystem   : ActorSystem = context.system
  var actDownload = context.actorOf(Props(new ClientDownActor()),gettingName)
  var actorGivers : List[(ActorRef,String,Int)] = List()
  var gettingCounter : Int = 0
  def getting(asender : ActorRef, srcPath: String, dstPath: String, numGetting: Int): Receive = {

    case SBProtocol.SendInfo(actTcp: ActorRef, ip: String, port: Int) => {
      requestSender = asender
      synchronized {
        gettingCounter += 1
        actorGivers = (actTcp, ip, port) :: actorGivers
        log.debug("SendInfo:: ip: %s, port: %d, counter: %d, requests: %d".format(ip,port,gettingCounter,numGetting))
        if (gettingCounter == numGetting) {
          startDownloading(srcPath, dstPath)
        }
      }
    }

    case SBProtocol.NotContain() =>
      synchronized {
        gettingCounter += 1
        if (gettingCounter == numGetting) {
          startDownloading(srcPath, dstPath)
        }
      }

    case _: Tcp.ConnectionClosed =>
      log.debug("Connection closed State CONNECTED")
      requestSender ! FSDistProtocol.CompletedUpload()
      context.become(connected())

    case SBInner.AddFile(path, info, token) =>
      log.debug("Add file - do nothing client")

    case unknown =>
      log.debug("State GETTING Unknown " + unknown )
      log.debug("State CONNECTED")
      context.become(connected())
  }

  def startDownloading(srcPath: String, dstPath: String): Unit = {
    actDownload ! SBInner.ClientSetPath(srcPath+dstPath)
    actDownload ! SBInner.SetRequestNumber(actorGivers.size)
    actorGivers.foreach {
      (actor : (ActorRef,String,Int)) => actDownload ! SBInner.Bind(self, actor._1 ,actor._2 , actor._3)
    }
    actorGivers = List()
    gettingCounter = 0
  }
}
