package client.model.distributed.routines

import akka.actor.{Actor, ActorRef}
import client.model.distributed.FSDistProtocol
import common.LoggerAspect
import scalbox.actors.protocols.SBProtocol

trait GettingFSRoutine {
  this: Actor =>

  val log           : LoggerAspect
  var requestSender : ActorRef
  def connected()   : Receive

  var nodeCounter   : Int = 0
  var files         : Map[String, (Long,String, Long,Byte)] = Map()

  val status_exist  : Byte = 'E'
  val default: (Long, String, Long, Byte) = (0, "", 0,'D')

  def gettingFS(sender : ActorRef, numNodes: Int): Receive = {
    case SBProtocol.ExtendedFiles(filesInfo) =>
      requestSender = sender
      if(handleFilesList(filesInfo, numNodes))
        context.become(connected())

    case unknown =>
      log.debug("State GETTING FS Unknown " + unknown )

      handleUnknown(unknown, numNodes)
      context.become(connected())

  }


  def handleFilesList(filesInfo: Map[String, (Long, String, Long, Byte)], numNodes: Int): Boolean =
  {
    log.debug("FilesList starts:: nodeCounter " + nodeCounter + " list size " + numNodes)

    log.debug("FilesList :: nodeCounter " + nodeCounter + " files " + filesInfo)
    //TODO do smt
    files = files ++ filesInfo.map{
      case (k,v) =>
      k -> { if(files.getOrElse(k,default)._3 <= v._3) v else files(k) }
    }
    nodeCounter += 1

    if(nodeCounter >= numNodes)  {
      log.debug("FilesList ends:: nodeCounter " + nodeCounter )
      nodeCounter = 0
      requestSender ! FSDistProtocol.Files(files.filter(p => p._2._4 == status_exist).map(p => (p._1, p._2._1, p._2._2)).toList)
      files = Map()
      return true
    }
    false
  }


  def handleUnknown(unknown: Any, size: Int): Unit = {
    log.warn("Getting file list Unknown message: " + unknown)

    if(nodeCounter < size) {
      nodeCounter += 1
    } else {
      nodeCounter = 0
    }
  }
}
