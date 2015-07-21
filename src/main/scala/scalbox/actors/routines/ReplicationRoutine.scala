package scalbox.actors.routines

import akka.actor.ActorRef
import akka.io.Tcp
import common.LoggerAspect
import scalbox.actors.SBServiceActor
import scalbox.actors.protocols.{SBInner, SBProtocol}


trait ReplicationRoutine {
  this: SBServiceActor =>

  val log           : LoggerAspect
  def normal()      : Receive

  val actDownload : ActorRef

  def getting(asender : ActorRef,  history: List[(String, (Long, Byte))], act: (String, (Long, Byte)), rootPath: String): Receive = {

    case SBProtocol.SendInfo(actTcp: ActorRef, ip: String, port: Int) => {
      log.debug("State replicate")

      //actDownload ! SBInner.ClientSetPath(rootPath + "/" + act._1)
      actDownload ! SBInner.ClientInfo(rootPath + "/" + act._1, act._2)
      actDownload ! SBInner.SetRequestNumber(1)
      actDownload ! SBInner.Bind(self, actTcp, ip, port)

    }

    case SBInner.Gossip() =>
      handleGossip()

    case _: Tcp.ConnectionClosed =>
      log.debug("Connection closed State NORMAL")
      val update = history.filter(p => p != act)
      log.debug("Update " + update)
      if(update.isEmpty) {
        log.debug("EndReplication " )

        asender ! SBInner.EndReplication()
        context.become(normal())
      }
      else {
        log.debug("Replicate " )

        asender ! SBProtocol.CanUpload("/" + update.head._1)
        context.become(getting(asender, update, update.head, rootPath))
      }

    case SBInner.AddFile(path, info, token) =>
      handleAddFile(path, info, token)

    case unknown =>
      log.debug("State REPLICATION Unknown " + unknown )
      log.debug("State NORMAL")
      context.become(normal())
  }
}

