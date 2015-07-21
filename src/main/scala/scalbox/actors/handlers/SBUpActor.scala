package scalbox.actors.handlers

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.Tcp.Register
import akka.io.{IO, Tcp}
import akka.util.ByteString
import common.LoggerAspect
import scalbox.actors.protocols.{SBInner, SBProtocol}

class SBUpActor(endpoint: InetSocketAddress)(implicit var actorSytem: ActorSystem) extends Actor {
  private val log = LoggerAspect(classOf[SBUpActor])
  var actTcpMaganger:ActorRef = null
  var actHandler:ActorRef = null
  var file_path: String = null
  var file_name: String = null
  var file_size: Long = 0
  var file_info:(Long,Byte) = null

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  override def receive: Receive = {
    case SBInner.SetFileInfo(name, path, size, info) =>
      handleSetFileInfo(name,path,size,info)
    case SBProtocol.Hand() =>
      handleHand()
    case c @ akka.io.Tcp.Connected(remote, local) =>
      handleTCPConnected(c, remote, local)
    case Register(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean) =>
      handleRegister(pHandler, keepOpenOnPeerClosed, useResumeWriting)
    case SBProtocol.Ack() =>
      handleAck()
    case _: Tcp.ConnectionClosed =>
      log.debug("Tcp.ConnectionClosed" + ", sender: " + sender)
    case unknown =>
      log.warn("Unknown message: " + unknown + ", sender: " + sender)
  }

  def handleSetFileInfo(name:String, path: String, size:Long, info:(Long,Byte)): Unit = {
    log.debug("Set file info %s, %s, %d, info %d".format(name,path,size, info._1))
    file_path = path
    file_name = name
    file_size = size
    file_info = info
  }

  def handleHand(): Unit = {
    log.debug("akka.io.Tcp.Connect" + ", sender: " + sender + " endpoint " + endpoint.toString())
    IO(Tcp) ! akka.io.Tcp.Connect(endpoint)
    actHandler = sender
  }

  def handleTCPConnected(c: Tcp.Connected, remote: InetSocketAddress, local: InetSocketAddress): Unit = {
    log.debug("akka.io.Tcp.Connected c=" + c.toString + ", sender: " + sender + " actHandler " + actHandler)
    actTcpMaganger = sender
    actHandler ! c
    actTcpMaganger ! akka.io.Tcp.Register(self)
  }

  def handleRegister(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean): Unit = {

    log.debug("TIMESTAMP: %d, path: %s. size %d ".format(file_info._1, file_path, file_size))
    val builder = ByteString.newBuilder
    builder.putLong(file_info._1)
    builder.putByte(file_info._2)

    log.debug("TIMESTAMP: %s ".format(builder.result()))

    actTcpMaganger ! Tcp.Write(builder.result())
    actTcpMaganger ! Tcp.WriteFile(file_path, 0, file_size, SBProtocol.Ack())

  }

  def handleAck(): Unit = {
    log.debug("Ack" + ", up sender: " + sender)
    actTcpMaganger ! Tcp.Close
  }
}
