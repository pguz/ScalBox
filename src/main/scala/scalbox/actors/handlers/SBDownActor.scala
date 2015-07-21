package scalbox.actors.handlers

import java.io.{File, PrintWriter}
import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import common.LoggerAspect
import scalbox.actors.protocols.{SBInner, SBProtocol}
import scalbox.model.Node

class SBDownActor(node: Node, endpoint: InetSocketAddress) extends Actor {

  private val log = LoggerAspect(classOf[SBDownActor])
  var file_path: String   = null
  var parent   : ActorRef = null

  var actTcpMaganger:ActorRef = null
  var actHandler:ActorRef = null
  implicit var actorSystem: ActorSystem = context.system
  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN
  var file_info:(Long,Byte) = null
  var token: Boolean = false
  var buffer: ByteString = ByteString()

  override def receive: Receive = {
    case SBInner.SetPath(path, info) =>
      handleSetPath(path, info)
    case SBProtocol.Hand() =>
      handleHand()
    case SBProtocol.Token() =>
      token = true
    case c @ akka.io.Tcp.Connected(remote, local) =>
      handleTCPConnected(c, remote, local)
    case Tcp.Register(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean) =>
      handleRegister(pHandler, keepOpenOnPeerClosed, useResumeWriting)
    case Tcp.Received(data) =>
      handleTCPReceived(data)
    case _: Tcp.ConnectionClosed =>
      handleTCPConnectionClosed()
    case SBProtocol.Ack() =>
      log.debug("Ack")
    case unknown =>
      log.warn("Unknown message: " + unknown + ", sender: " + sender)

  }

  def handleHand(): Unit = {
    log.debug("akka.io.Tcp.Connect" + ", sender: " + sender)
    IO(Tcp) ! akka.io.Tcp.Connect(endpoint)
    actHandler = sender
  }

  def handleTCPConnected(c: Tcp.Connected, remote: InetSocketAddress, local: InetSocketAddress): Unit = {
    log.debug("akka.io.Tcp.Connected c=" + c.toString + ", sender: " + sender + " actHandler " + actHandler)
    actTcpMaganger = sender
    actHandler ! c
    actTcpMaganger ! akka.io.Tcp.Register(self)
  }

  def handleSetPath(path: String,info:(Long,Byte)): Unit = {
    log.debug("Set path " + path + " timestamp " + info._1 + " status " + info._2.toChar)
    parent = sender()
    file_path = path
    file_info = info
  }

  def handleTCPReceived(data: ByteString): Unit = {
    buffer = buffer ++ data
    log.debug("Received" + ", sender: " + sender + " ,self " + self + " buffer " + buffer)
  }

  def handleRegister(pHandler: ActorRef, keepOpenOnPeerClosed: Boolean, useResumeWriting: Boolean): Unit = {
    implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN
    val byte :Byte = 'E'
    val builder = ByteString.newBuilder
    builder.putByte(byte)

    log.debug("Byte: %s ".format(builder.result()))

    actTcpMaganger ! Tcp.Write(builder.result(), SBProtocol.Ack())
  }

  def writeFile() {

    val iterator = buffer.iterator
    val timestamp: Long = iterator.getLong
    val status   : Byte = iterator.getByte
    val name_size: Int = iterator.getInt
    val array_name =  new Array[Byte](name_size)
    iterator.getBytes(array_name)
    val name     : String = new String(array_name)

    log.debug("DATA " + buffer)

    if(file_info._2 < timestamp) {
      try {
        val data = iterator.toSeq.utf8String.trim()
        val pw = new PrintWriter(new File(file_path + name))
        log.debug("name %s, status %s, value %s, path %s".format(name, status.toChar, data, file_path + name))
        pw.write(data)
        pw.close()
      } catch {
        case e: Exception => log.debug("Tcp.Received" + e.getMessage + " path: " + file_path)
      }
    }
    parent ! SBInner.AddFile(name,(timestamp,status), token)
    token = false
    buffer = ByteString()
  }

  def handleTCPConnectionClosed(): Unit = {
    log.debug("Tcp.ConnectionClosed" + ", sender: " + sender)
    writeFile()
    sender ! Tcp.Closed
  }
}
