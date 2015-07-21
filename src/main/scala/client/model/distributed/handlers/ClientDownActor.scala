package client.model.distributed.handlers

import java.io.{File, PrintWriter}
import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.{IO, Tcp}
import akka.util.{ByteIterator, ByteString}
import common.LoggerAspect
import common.filesystem.FileStatus
import scalbox.actors.protocols.{SBInner, SBProtocol}

class ClientDownActor() extends Actor {

  private val log = LoggerAspect(classOf[ClientDownActor])
  var numRequest: Int     = 0
  var file_path: String   = null
  var file_timestamp: Long   = 0
  var file_status: Byte   = FileStatus.unknown
  var listener : List[ActorRef] = List()
  var parent   : ActorRef = null
  var actServerHandler: List[ActorRef] = List()
  var endpoint: InetSocketAddress = null
  implicit var actorSystem: ActorSystem = context.system
  var countTimestamp: Int = 0
  var countClosed   : Int = 0
  var countBound   : Int = 0
  var buffer: Map[ActorRef,ByteString] = Map()

  override def receive: Receive = {
    case SBInner.ClientInfo(name, info) =>
      println("SBInner.ClientInfo")
      handleSetInfo(name, info)
    case SBInner.ClientSetPath(path) =>
      handleSetPath(path)
    case SBInner.SetRequestNumber(num) =>
      handleSetRequestNum(num)
    case SBInner.Bind(asender, server, ip, port) =>
      handleBind(asender, server, ip, port)
    case Tcp.Connected(remote, local) =>
      handleTCPConnected(remote, local)
    case Tcp.Bound(localAddress: InetSocketAddress) =>
      handleTCPBound(localAddress)
    case Tcp.Register(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean) =>
      log.debug("Tcp.Register ")
    case Tcp.Received(data) =>
      handleTCPReceived(data)
    case _: Tcp.ConnectionClosed =>
      handleTCPConnectionClosed()
    case Tcp.Unbound =>
      log.debug("Tcp.Unbound ")
    case Tcp.Unbind =>
      log.debug("Tcp.Unbind ")
    case unknown =>
      log.warn("Unknown message: " + unknown + ", sender: " + sender)
  }

  def handleBind(actor: ActorRef, server: ActorRef, ip: String, port: Int) {
    log.debug("Bind::start; parent" + actor + " server " + server + " self " + self + " ip %s, port %d".format(ip,port))
    parent = actor
    IO(Tcp) ! Tcp.Bind(self,new InetSocketAddress(ip,port))
    actServerHandler = server :: actServerHandler
    log.debug("Bind::ends")
  }

  def handleTCPConnected(remote: InetSocketAddress, local: InetSocketAddress): Unit = {
    log.debugDescAsp("Tcp.Connected:: remote: " + remote + ", local: " + local + ", sender: " + sender
      + " self " + self,
      sender ! Tcp.Register(self))
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

  def handleSetPath(name: String): Unit = {
    log.debug("Set path " + name)
    file_path = name
  }

  def handleSetInfo(name: String, info: (Long,Byte)): Unit = {
    log.debug("Set path " + info._1 )
    file_path = name
    file_timestamp = info._1
    file_status = info._2
  }

  def handleSetRequestNum(num: Int): Unit = {
    log.debug("Set num of request " + num )
    numRequest = num
  }



  def handleTCPReceived(data: ByteString): Unit = {
    this.synchronized {
        buffer = buffer + (sender() -> buffer.getOrElse(sender(),ByteString()).concat(data))
    }
  }

  def writeFile() {
    implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

    buffer foreach (e => log.debug("DATA: " + e._1 + " " + e._2))
    val timestamp: Map[Long, ByteIterator] = buffer.values.map(data => {
      val split = data.splitAt(8)
      split._1.iterator.getLong -> split._2.iterator }).toMap

    try {
      val max = timestamp.keys.max
      val iterator = timestamp(max)
      val status = iterator.getByte
      if(status == 'E') {
        val data = iterator.toSeq.utf8String.trim()
        val pw = new PrintWriter(new File(file_path))
        log.debug("max " + max + " value " + data + " status " + status.toChar)
        pw.write(data)
        pw.close()
        parent ! SBInner.AddFile(file_path.split("/").last,(file_timestamp, file_status), false)
      }
    } catch {
      case e: Exception => log.debug("Tcp.Received" + e.getMessage + " path: " + file_path)
    }

    buffer = Map()
  }

  def handleTCPConnectionClosed(): Unit = {
    log.debug("Tcp.ConnectionClosed" + ", sender: " + sender)
    countClosed += 1
    if(countClosed == numRequest) {
      log.debug("Tcp.ConnectionClosed" + ", sender: " + sender)

      writeFile()
      listener.foreach((actor: ActorRef) => actor ! Tcp.Unbind)
      listener = List()
      parent ! Tcp.Closed
      countClosed = 0
      actServerHandler = List()
    }
  }
}
