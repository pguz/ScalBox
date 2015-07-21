package client.controller

import _root_.common.LoggerAspect
import client.controller.distributed.DistController
import client.controller.local.LocalController
import client.model.ClientModel
import client.view.ClientView
import rx.lang.scala.Observable

trait ClientController extends LocalController with DistController {
  self: ClientModel with ClientView =>

  private val log = LoggerAspect(classOf[ClientController])

  val obsUploadFromLocal: Observable[((String, String, Long), String)]
  = obsBtnLocalCopy.filter(_ => selectedOneRowLocalPnl && selectedOneRowDistPnl)
    .map(_ => ((getSelLocalFile, getSelLocalPath, getSelLocalFileSize), getSelDistPath))

  val obsDownloadFromDist: Observable[(String, String)]
  = obsBtnDistCopy.filter(_ => selectedOneRowDistPnl)
    .map(_ => (getLocalPath, getSelDistPath))

  val obsPathRefresh: Observable[String]
  = obsBtnRefresh.map(_ => getDistPath)

  obsUploadFromLocal.subscribe(elem => uploadFile(elem._1, elem._2))
  obsDownloadFromDist.subscribe(elem => downloadFile(elem._1, elem._2))
  obsPathRefresh.subscribe(getFiles(_))

  def uploadFile(srcInfo: (String, String, Long), dstInfo: String): Unit = {
    log.info("Upload files " + srcInfo + " " + dstInfo)
    uploadDistFile(srcInfo, dstInfo)
  }

  def downloadFile(srcInfo: String, dstInfo: String): Unit = {
    log.info("Download files " + srcInfo + " " + dstInfo)
    downloadDistFile(srcInfo, dstInfo, updateLocal)
  }

  def getFiles(path: String): Unit = {
    getDistFiles(getDistPath)
    getLocalFiles(getLocalPath)
  }

  def updateLocal(): Unit ={
    getLocalFiles(getLocalPath)
  }

}
