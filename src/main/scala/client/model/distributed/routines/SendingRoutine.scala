package client.model.distributed.routines

import akka.actor._
import akka.io.Tcp
import client.model.distributed.{DistFSActor, FSDistProtocol}
import client.model.distributed.handlers.ClientUpActor
import com.typesafe.config.{Config, ConfigFactory}
import common.LoggerAspect
import scalbox.actors.protocols.{SBInner, SBProtocol}

trait SendingRoutine {

  this: DistFSActor =>

  val log           : LoggerAspect
  var requestSender : ActorRef
  def connected()   : Receive

  private val configMainClient : Config  = ConfigFactory.load().getConfig("client")
  private val config           : Config  = ConfigFactory.load(configMainClient.getString("config"))

  val sendingName   : String = config.getString("system.sendingName")
  var sendingCounter: Int    = 0
  var actUploaders : List[(ActorRef,String,Int)] = List()

  val actUpload = context.actorOf(Props(new ClientUpActor()), sendingName)
  implicit val actorSystem   : ActorSystem = context.system

  def sending(qsender : ActorRef, name: String, path: String, size: Long, numRequest: Int): Receive = {

    case SBProtocol.SendInfo(actTcp, ip, port) => {
      requestSender = qsender
      synchronized {
        sendingCounter += 1
        actUploaders = (actTcp, ip, port) :: actUploaders
        log.debug("SendInfo:: ip: %s, port: %d, counter: %d, requests: %d".format(ip,port,sendingCounter,numRequest))
        if (sendingCounter == numRequest) {
          startUploading(name, path, size)
        }
      }
    }

    case _: Tcp.ConnectionClosed =>
      log.debug("Connection closed State CONNECTED")
      requestSender ! FSDistProtocol.CompletedUpload()
      context.become(connected())

    case unknown =>
      log.debug("State GETTING Unknown " + unknown )
      log.debug("State CONNECTED")
      context.become(connected())
  }

  def startUploading(name: String, path: String, size: Long): Unit = {
    actUpload ! SBInner.ClientSetFileInfo(name, path, size)
    actUpload ! SBInner.SetTimestamp(System.currentTimeMillis())
    actUpload ! SBInner.SetRequestNumber(actUploaders.size)
    actUploaders.foreach {
      (actor : (ActorRef,String,Int)) => actUpload ! SBInner.Bind(self, actor._1 ,actor._2 , actor._3)
    }
    actUploaders = List()
    sendingCounter = 0
  }
}
