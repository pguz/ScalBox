package client

import client.controller.ClientController
import client.model.ClientModel
import client.view.ClientView

import scala.swing.SimpleSwingApplication

object ClientApp extends SimpleSwingApplication {
  def top = (new ClientApp).view("ScalBox")
}

class ClientApp()
  extends ClientModel()
  with ClientController
  with ClientView