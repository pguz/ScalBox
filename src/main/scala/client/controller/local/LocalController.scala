package client.controller.local

import client.model.ClientModel
import client.view.ClientView
import common.filesystem.FSHelper
import rx.lang.scala.Observable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait LocalController extends FSHelper {
  self: ClientModel with ClientView =>

  //references to gui objects
  val pnlLocal = pnlMain.pnlLocal
  val tblLocal = pnlLocal.pnlFiles.pneFiles.tblfiles
  val mdlLocal = tblLocal.model

  def getLocalPath: String
    = pnlLocal.pnlPath.pthFile.text
  def getSelLocalFile: String
    = mdlLocal.getValueAt(tblLocal.peer.getSelectedRow, 0).toString
  def getSelLocalFileSize: Long
    = mdlLocal.getValueAt(tblLocal.peer.getSelectedRow, 1).toString.toLong
  def getValLocalFile(row: Int): String
    = getLocalPath + "/" + mdlLocal.getValueAt(row, 0).toString
  def getSelLocalPath: String
    = getLocalPath + "/" + getSelLocalFile
  def selectedOneRowLocalPnl: Boolean
    = tblLocal.peer.getSelectedRowCount == 1

  def setLocalPath(s: String): Unit
    = pnlLocal.pnlPath.setPathFile(s)
  def updateLocalPanel(path: String, fileInfos: List[(String,Long,String)]): Unit
    = tblLocal.updatePane(path, fileInfos)
  def setLocalStatus(s: String)
    = pnlLocal.pnlStatus.setText(s)

  /* !!!!!!!!!! GUI STREAMS !!!!!!!!!! */
  //stream clicking button ^
  val obsLocalBtnUp:  Observable[Boolean]
    = pnlLocal.pnlPath.obsClickUp

  //stream clicking row in dist file table
  val obsLocalPnlFile: Observable[Int]
    = pnlLocal.pnlFiles.pneFiles.obsFilesChoosen

  val obsLocalBtnDelete: Observable[Boolean]
    = pnlLocal.pnlCommands.obsClickDelete

  /* !!!!!!!!!! LOGIC STREAMS !!!!!!!!!! */
  val obsLocalPathParent: Observable[String]
    = obsLocalBtnUp.map(_ => reducePath(getLocalPath, pthLclStop)).collect{case Some(validpath) => validpath}

  val obsLocalFileSelected: Observable[String]
    = obsLocalPnlFile.map(getValLocalFile(_))

  val obsLocalDelete: Observable[String]
    = obsLocalBtnDelete.filter(_ => selectedOneRowLocalPnl)
    .map(_ => getSelLocalPath)

  /* !!!!!!!!!! OBSLUGA STRUMIENI !!!!!!!!!! */
  obsLocalPathParent.subscribe(getLocalFiles(_))
  obsLocalFileSelected.subscribe(getLocalFiles(_))
  obsLocalDelete.subscribe(deleteLocalFile(_))

  def getLocalFiles(path: String) {
    fGetFiles(path) onComplete {
      case Failure(t) =>
        setLocalStatus(s"Could not get files: $t")
      case Success(fileInfos: List[(String,Long,String)]) =>
        setLocalStatus(s"Get Files!")
        setLocalPath(path)
        println("setLocalPath: " + path)
        updateLocalPanel(path, fileInfos)}}

  def deleteLocalFile(path: String): Unit = {
    fDeleteFile(path) onComplete {
      case Failure(err) =>
        setLocalStatus(s"Could not delete file: $err")
      case Success(false) =>
        setLocalStatus(s"Could not delete file!")
      case Success(true) =>
        setLocalStatus(s"File deleted")
        Thread.sleep(100)
        getLocalFiles(getLocalPath)}}

  setLocalPath(pthLclDef)
  getLocalFiles(pthLclDef)
}
