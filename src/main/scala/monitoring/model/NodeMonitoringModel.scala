package monitoring.model

import java.io.{BufferedReader, File, FileReader}
import java.util.Date

class NodeMonitoringModel(
  val path: String,
  val config: String) {
  val configPath = path + '/' + config

  def initReadConfig: String = {
    var res: String = ""
    val in: BufferedReader = new BufferedReader(new FileReader(new File(configPath)))
    var line: String = in.readLine();
    while(line != null){
      res += line + '\n'
      line = in.readLine();
    }
    res
  }

  def getFileList: List[(String, Long, String)] = {
    (new File(path)).listFiles().map(f => (f.getName, f.length(), (new Date(f.lastModified)).toString)).toList
  }

}
