package client.model.distributed.handlers

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import common.LoggerAspect
import scalbox.actors.protocols.{SBInner, SBProtocol}

class ClientUpActor() extends Actor {
  private val log = LoggerAspect(classOf[ClientUpActor])

  implicit var actorSystem: ActorSystem = context.system

  var actHandler:ActorRef = null
  var parent   : ActorRef = null
  var actServerHandler: List[ActorRef] = List()
  var listener : List[ActorRef] = List()

  var file_path: String = null
  var file_name: String = null
  var file_size: Long = 0
  var timestamp :Long = 0
  val status :Byte = 'E'

  var numRequest: Int     = 0
  var countClosed   : Int = 0
  var countAck   : Int = 0
  var countBound   : Int = 0

  override def receive: Receive = {
    case SBInner.ClientSetFileInfo(name,path,size) =>
      handleSetFileInfo(name,path,size)
    case SBInner.SetRequestNumber(num) =>
      handleSetRequestNum(num)
    case SBInner.SetTimestamp(client_timestamp) =>
      handleSetTimestamp(client_timestamp)

    case SBInner.Bind(asender, server, ip, port) =>
      handleBind(asender, server, ip, port)
    case Tcp.Bound(localAddress: InetSocketAddress) =>
      handleTCPBound(localAddress)
    case Tcp.Register(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean) =>
      log.debug("Tcp.Register ")
    case Tcp.Connected(remote, local) =>
      handleTCPConnected(remote, local)

    case Tcp.Received(data) =>
      handleReceived(sender())

    case _: Tcp.ConnectionClosed =>
      handleTCPConnectionClosed()
    case Tcp.Unbound =>
      log.debug("Tcp.Unbound ")
    case Tcp.Unbind =>
      log.debug("Tcp.Unbind ")
    case SBProtocol.Ack() =>
      handleAck()
    case unknown =>
      log.warn("Unknown message: " + unknown + ", sender: " + sender)
  }

  def handleSetRequestNum(num: Int): Unit = {
    log.debug("Set num of request " + num )
    numRequest = num
  }

  def handleSetFileInfo(name:String, path: String, size:Long): Unit = {
    log.debug("Set file info %s, %s, %d".format(name,path,size))
    file_path = path
    file_name = name
    file_size = size
  }

  def handleBind(actor: ActorRef, server: ActorRef, ip: String, port: Int) {
    log.debug("Bind::start; parent" + actor + " server " + server + " self " + self + " ip %s, port %d".format(ip,port))
    parent = actor
    IO(Tcp) ! Tcp.Bind(self,new InetSocketAddress(ip,port))
    actServerHandler = server :: actServerHandler
    log.debug("Bind::ends")
  }

  def handleTCPBound(localAddress: InetSocketAddress): Unit = {
    log.debug("Tcp.Bound:: localAddress: " + localAddress.getAddress + ", sender: " + sender)
    listener = sender() :: listener
    countBound += 1
    if(countBound == numRequest) {
      actServerHandler.reverse.head ! SBProtocol.Token()
      actServerHandler.foreach(actor => actor ! SBProtocol.Hand())
      countBound = 0
    }
  }

  def handleSetTimestamp(client_timestamp: Long): Unit = {
    log.debug("Set timestamp %d".format(client_timestamp) )
    timestamp = client_timestamp
  }

  def handleTCPConnected(remote: InetSocketAddress, local: InetSocketAddress): Unit = {
    log.debug("Tcp.Connected:: remote: " + remote + ", local: " + local + ", sender: " + sender  + " self " + self)
    sender ! Tcp.Register(self)
  }

  /* status:
   * 'E' - exist
   * 'D' - deleted
   */
  def handleReceived(pHandler: ActorRef): Unit = {
    implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

    log.debug(" pHandler " + pHandler + " sender " + sender + " actHandler " + actHandler + " actServerHandler " + actServerHandler + " self " + self )
    log.debug("TIMESTAMP: %d, path: %s. size %d ".format(timestamp, file_path, file_size))
    val builder = ByteString.newBuilder
    builder.putLong(timestamp)
    builder.putByte(status)
    builder.putInt(file_name.size)
    builder.putBytes(file_name.getBytes)

    log.debug("TIMESTAMP: %s ".format(builder.result()))

    pHandler ! Tcp.Write(builder.result())
    pHandler ! Tcp.WriteFile(file_path, 0, file_size, SBProtocol.Ack())

  }

  def handleAck(): Unit = {
    log.debug("Ack" + ", up sender: " + sender)
    countAck += 1
    if(countAck == numRequest) {
      actServerHandler.foreach(actor => actor ! Tcp.Closed)
      countAck = 0
      actServerHandler = List()
    }
  }

  def handleTCPConnectionClosed(): Unit = {
    log.debug("Tcp.ConnectionClosed" + ", sender: " + sender)
    countClosed += 1
    if(countClosed == numRequest) {
      listener.foreach((actor: ActorRef) => actor ! Tcp.Unbind)
      listener = List()
      parent ! Tcp.Closed
      countClosed = 0
    }
  }
}
