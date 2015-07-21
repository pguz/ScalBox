package scalbox.model

import common.filesystem.DistFS

object Node {
  def apply(
    name: String,
    ip: String,
    portDown: Int,
    portUp: Int,
    configName: String,
    pthDef: String,
    pthStop: String) = new Node(name, ip, portDown, portUp, configName, pthDef, pthStop)
}

class Node(
  val name:       String,
  val ip:         String,
  val portDown:   Int,
  val portUp:     Int,
  val configName: String,
  val pthDef:     String,
  val pthStop:    String) {

  val fs = new DistFS(pthDef, configName)
  fs.init()
}
