package monitoring.controller

import java.io.File

import monitoring.model.NodeMonitoringModel
import monitoring.view.NodeMonitoringView

trait NodeMonitoringController {
  self: NodeMonitoringModel with NodeMonitoringView =>

  def checkPath: Boolean = (new File(path)).exists()
  def checkConfig: Boolean = (new File(configPath)).exists()

  def checkConsistency: Boolean
    = checkPath && checkConfig

  def init: Unit = {
    updateFilesPanel
    initFillConfigArea
  }

  def update: Unit = {
    updateFilesPanel
    initFillConfigArea
  }

  def updateFilesPanel: Unit
    = PaneFiles.updatePane(getFileList)

  def initFillConfigArea: Unit = {
    PnlConfig.fill(initReadConfig)
  }


}
