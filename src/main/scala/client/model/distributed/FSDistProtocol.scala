package client.model.distributed

object FSDistProtocol {
  sealed trait FSDistRequest
  case class Connect()
    extends FSDistRequest
  case class Disconnect()
    extends FSDistRequest
  case class GetFileList(path: String, numNodes: Int)
    extends FSDistRequest
  case class SendFile(srcFile: (String, String, Long), dstPath: String, numNodes: Int)
    extends FSDistRequest
  case class GetFile(srcPath: String, dstPath: String)
    extends FSDistRequest
  case class DeleteFile(path: String, numNodes: Int)
    extends FSDistRequest

  sealed trait FSDistResponse
  case class Connecting()
    extends FSDistResponse
  case class Connected(numNodes: Int)
    extends FSDistResponse
  case class Disconnected()
    extends FSDistResponse
  case class Files(filesInfo: List[(String,Long,String)])
    extends FSDistResponse
  case class CompletedUpload()
    extends FSDistResponse
  case class Deleted()
    extends FSDistResponse
}
