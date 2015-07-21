package client.controller.distributed

import client.model.ClientModel
import client.view.ClientView
import common.filesystem.FSHelper
import rx.lang.scala.Observable
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

trait DistController extends FSHelper {
  self: ClientModel with ClientView =>

  //references to gui objects
  val pnlDist = pnlMain.pnlDist
  val logDist = pnlMain.pnlLog
  val tblDist = pnlDist.pnlFiles.pneFiles.tblfiles
  val mdlDist = tblDist.model

  def getDistPath: String
    = pnlDist.pnlPath.pthFile.text
  def getSelDistFile: String
    = mdlDist.getValueAt(tblDist.peer.getSelectedRow, 0).toString
  def getValDistFile(row: Int): String
    = mdlDist.getValueAt(row, 0).toString
  def getSelDistPath: String
    = getDistPath + "/" + getSelDistFile
  def selectedOneRowDistPnl: Boolean
    = tblDist.peer.getSelectedRowCount == 1
  def getSelectedNodes: Int
    = (pnlDist.pnlSettings.spnNodes.getValue).asInstanceOf[Int]

  def setDistPath(s: String): Unit
    = pnlDist.pnlPath.setPathFile(s)
  def updateDistPanel(path: String, fileInfos: List[(String,Long,String)]): Unit
    = tblDist.updatePane(path, fileInfos)
  def setDistStatus(s: String)
    = pnlDist.pnlStatus.setText(s)
  def setMaxNodes(n: Int)
    = pnlDist.pnlSettings.setMaxNodes(n)
  def setNodes(n: Int)
    = pnlDist.pnlSettings.setNodes(n)

  //flip flop on connect button
  var maskConnect = true

  /* !!!!!!!!!! GUI STREAMS !!!!!!!!!! */

  //stream clicking button ^
  val obsBtnDistUp:  Observable[Boolean]
    = pnlDist.pnlPath.obsClickUp

  //stream clicking row in dist file table
  val obsPnlDistFile: Observable[Int]
    = pnlDist.pnlFiles.pneFiles.obsFilesChoosen

  //stream clicking button Connect
  val obsBtnConnect:  Observable[Boolean]
    = pnlDist.pnlCommands.obsClickConnect

  //stream clicking button Refresh
  val obsBtnRefresh:  Observable[Boolean]
    = pnlDist.pnlCommands.obsClickRefresh

  val obsBtnDistDelete: Observable[Boolean]
    = pnlDist.pnlCommands.obsClickDelete

  //stream clicking button <-Copy
  val obsBtnLocalCopy: Observable[Boolean]
  = pnlMain.pnlLocal.pnlCommands.obsClickCopy

  //stream clicking button <-Copy
  val obsBtnDistCopy: Observable[Boolean]
  = pnlDist.pnlCommands.obsClickCopy

  /* !!!!!!!!!! LOGIC STREAMS !!!!!!!!!! */
  val obsPathParent: Observable[String]
    = obsBtnDistUp.map(_ => reducePath(getDistPath, pthDistStop)).collect{case Some(validpath) => validpath}

  val obsFileSelected: Observable[String]
    = obsPnlDistFile.map(getValDistFile(_))

  val obsConnect: Observable[Boolean]
    = obsBtnConnect.map(_ & maskConnect)


  val obsDelete: Observable[String]
    = obsBtnDistDelete.filter(_ => selectedOneRowDistPnl)
    .map(_ => getSelDistPath)

  /* !!!!!!!!!! STREAM OBSERVERS !!!!!!!!!! */
  obsPathParent.subscribe(getDistFiles(_))
  obsFileSelected.subscribe(getDistFiles(_))
  obsConnect.subscribe( _ match {
    case true => tryConnect()
    case false => disconnect()
  })
  obsDelete.subscribe(deleteDistFile(_))


  /* !!!!!!!!!! AKCJE !!!!!!!!!! */

  def getDistFiles(path: String): Unit = {
    logDist.fill("Sending to " + getSelectedNodes + " nodes\n")
    fGetFiles(path, getSelectedNodes) onComplete {
      case Failure(t) =>
        setDistStatus(s"Could not get files: $t")
      case Success(fileInfos: List[(String,Long,String)]) =>
        setDistStatus(s"Get Files!")
        setDistPath(path)
        logDist.fill("Received files from " + getSelectedNodes + " nodes\n")
        updateDistPanel(path, fileInfos)}}

  def tryConnect(): Unit = {
    logDist.fill("Trying to connect to SBDist\n")
    fConnect onComplete {
      case Failure(t) =>
        setDistStatus(s"Could not connect: $t")
        disconnected()
      case Success(0) =>
        setDistStatus(s"Could not find server.")
        disconnected()
      case Success(n) =>
        setDistStatus("Connected!")
        logDist.fill("Connected to " + n + " nodes\n")
        setMaxNodes(n)
        setNodes(n)
        numNodes = n
        connected()}}

  def disconnect(): Unit = {
    fDisconnect onComplete  {
      case Failure(err) =>
        setDistStatus(s"Could not disconnect: $err")
      case Success(false) =>
        setDistStatus("Could not disconnect!")
        disconnected()
      case Success(true) =>
        setDistStatus("Disconnected!")
        disconnected()}}

  def deleteDistFile(path: String): Unit = {
    println("deleteDistFile")
    val numNodes = getSelectedNodes
    fDeleteFile(path, numNodes) onComplete {
      case Failure(err) =>
        setDistStatus(s"Could not delete file: $err")
      case Success(false) =>
        setDistStatus(s"Could not delete file!")
      case Success(true) =>
        logDist.fill("File " + path + " deleted from " + numNodes + " nodes.\n")
        getDistFiles(getDistPath)}}

  def uploadDistFile(srcInfo: (String, String, Long), dstInfo: String): Unit = {
    println("uploadDistFile")
    val numNodes = getSelectedNodes
    fCopyFile(srcInfo, dstInfo, numNodes) onComplete {
      case Failure(t)     =>
        setDistStatus(s"Could not upload file: $t")
      case Success(false) =>
        setDistStatus(s"Could not upload file")
      case Success(true)  =>
        logDist.fill("File " + srcInfo._1 + " was sent to " + numNodes + " nodes.\n")
        setDistStatus(s"File has been uploaded")
        getDistFiles(pthDistAct)}}

  def downloadDistFile(srcInfo: String, dstInfo: String, updateLocal:() => Unit): Unit = {
    fGetFile(srcInfo, dstInfo) onComplete {
      case Failure(t)     =>
        setDistStatus(s"Could not upload file: $t")
      case Success(false) =>
        setDistStatus(s"Could not upload file")
      case Success(true)  =>
        println("Local file download")
        setDistStatus(s"File has been uploaded")
        updateLocal()
    }}

  /* !!!!!!!!!! REAKCJE !!!!!!!!!! */

  def connected(): Unit = {
    maskConnect = false
    pnlDist.enable()
    pnlMain.pnlLocal.pnlCommands.btnCopy.enabled = true
    Thread.sleep(100)
    getDistFiles(getDistPath)
  }

  def disconnected(): Unit = {
    pnlMain.pnlLocal.pnlCommands.btnCopy.enabled = false
    maskConnect = true
    pnlDist.disable()
  }
}
