package monitoring

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import monitoring.controller.NodeMonitoringController
import monitoring.model.NodeMonitoringModel
import monitoring.view.NodeMonitoringView

import scala.swing.{Frame, SimpleSwingApplication}

object NodeMonitoringApp extends App {

  override def main (args: Array[String]) {
    val app = returnApp(args)
    if(app == null)
      return

    init(app)
    run(app)
  }

  class NodeMonitoringSwingApp(f: Frame) extends SimpleSwingApplication {
    override def top: Frame = f
  }

  object Logger {
    val usage: String   = "Usage: path title"
    val pathNotExists   = "Provided path not exists"
    val configNotExists = "Provided config not exists"
    val argsWrong       = "Problems with provided arguments"
  }

  def returnApp(args: Array[String]): NodeMonitoringApp = {
    if(args.length != 3) {
      println(Logger.usage)
      return null
    }

    val path        = args(0)
    val appName     = args(1)
    val configName  = args(2)
    val app         = new NodeMonitoringApp(appName, path, configName)

    if(!app.checkConsistency){
      println(Logger.argsWrong)
      return null
    }
    app
  }

  def init(app: NodeMonitoringApp) = {
    app.init
    super.main(args)
    val swingApp = new NodeMonitoringSwingApp(app.view(app.name))
    swingApp.main(args)
    swingApp.top
  }

  def run(app: NodeMonitoringApp) = {
    val timeDelay: Int    = 0
    val timeInterval: Int = 1

    val updateApp: Runnable = new Runnable {
      override def run(): Unit = {
        app.update
      }
    }

    val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    executor.scheduleAtFixedRate(updateApp, timeDelay, timeInterval, TimeUnit.SECONDS)
  }
}

class NodeMonitoringApp(val name: String, path: String, config: String)
  extends NodeMonitoringModel(path, config)
  with NodeMonitoringController
  with NodeMonitoringView