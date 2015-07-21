package client.view

import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultCaret
import javax.swing.{JSpinner, SpinnerNumberModel}

import common.SwingUtility
import rx.lang.scala.Observable

import scala.swing.BorderPanel.Position._
import scala.swing.ScrollPane.BarPolicy
import scala.swing._

trait ClientView extends SwingUtility {

  def view(s: String) = new View(s)

  class View(val frameTitle: String) extends MainFrame {
    title = frameTitle

    contents = new BorderPanel {
      layout(pnlMain) = Center
    }
  }

  object pnlMain extends GridPanel(1,3) {
    val pnlLocal  = new LocalFilesystem
    val pnlDist   = new DistFilesystem
    val pnlLog    = new PnlLogger
    contents += pnlLocal += pnlDist += pnlLog
  }

  abstract class PanelFilesystem extends BorderPanel {
    object pnlPath extends BorderPanel {
      val lblPath = new Label("Path:")
      val pthFile = new TextField("") { editable = false }
      val btnUp = new Button("^")
      val obsClickUp: Observable[Boolean] = btnUp.clicks

      def setPathFile(s: String) = swing {
        pthFile.text = s
      }

      layout(lblPath) = West
      layout(pthFile) = Center
      layout(btnUp) = East
    }

    object pnlFiles extends BorderPanel {
      object pneFiles extends ScrollPane {
        val columnNames =
          Array[AnyRef]("Filename", "Size", "Date modified")
        val tblfiles = new Table {
          showGrid = true
          model = new DefaultTableModel(columnNames, 0) {
            override def isCellEditable(r: Int, c: Int) = false
          }
          selection.intervalMode = Table.IntervalMode.Single

          val colSize = peer.getColumn("Size")
          colSize.setMinWidth(40)
          colSize.setPreferredWidth(40)
          colSize.setMaxWidth(40)

          val colTimestamp = peer.getColumn("Date modified")
          colTimestamp.setMinWidth(250)
          colTimestamp.setPreferredWidth(250)
          colTimestamp.setMaxWidth(250)

          def updatePane(dir: String, files: List[(String,Long,String)]) = swing {
            model match {
              case d: DefaultTableModel =>
                d.setRowCount(0)
                files.foreach(f => {
                  val array: Array[AnyRef] = Array[AnyRef](f._1, f._2.toString, f._3)
                  d.addRow(array)
                })
            }
          }
        }
        val obsFilesChoosen: Observable[Int] = tblfiles.rowDoubleClicks

        contents = tblfiles
      }
      layout(pneFiles) = Center
    }

    object pnlStatus extends BorderPanel {
      val lbl = new Label("...", null, Alignment.Left)
      layout(new Label("Status: ")) = West
      layout(lbl) = Center
      def setText(s: String) = swing {
        lbl.text = s
      }
    }

  }

  class LocalFilesystem extends PanelFilesystem {
    object pnlCommands extends GridPanel(1, 2) with SwingUtility {
      val btnCopy = new Button("Copy ->")
      val btnDelete = new Button("Delete")

      val obsClickCopy: Observable[Boolean] = btnCopy.clicks
      val obsClickDelete: Observable[Boolean] = btnDelete.clicks
      contents += btnDelete += btnCopy

      def disable(): Unit = swing {
        btnCopy.enabled = false
        btnDelete.enabled = false
      }

      def enable(): Unit = swing {
        btnCopy.enabled = true
        btnDelete.enabled = true
      }
    }

    object pnlMain extends BorderPanel {
      layout(pnlPath) = North
      layout(pnlFiles) = Center
      layout(pnlCommands) = South
    }

    layout(pnlMain) = Center
    layout(pnlStatus) = South


    def enable(): Unit =
      pnlCommands.enable()

    def disable(): Unit =
      pnlCommands.disable()

    enable
  }

  class DistFilesystem extends PanelFilesystem {
    object pnlCommands extends GridPanel(2, 2)  with SwingUtility  {
      val btnConnect = new Button("Connect")
      val btnRefresh = new Button("Refresh")
      val btnCopy = new Button("<- Copy")
      val btnDelete = new Button("Delete")

      val obsClickConnect: Observable[Boolean] = btnConnect.clicks
      val obsClickRefresh: Observable[Boolean] = btnRefresh.clicks

      val obsClickCopy: Observable[Boolean] = btnCopy.clicks
      val obsClickDelete: Observable[Boolean] = btnDelete.clicks

      def disable(): Unit = swing {
        btnCopy.enabled = false
        btnDelete.enabled = false
      }

      def enable(): Unit = swing {
        btnCopy.enabled = true
        btnDelete.enabled = true
      }

      contents += btnConnect += btnRefresh += btnCopy += btnDelete
    }

    object pnlSettings extends GridPanel(1, 1) {
      val spnNodesMdl = new SpinnerNumberModel(0, 0, 0, 1)
      val spnNodes = new JSpinner(spnNodesMdl)
      spnNodes.setEditor(new JSpinner.DefaultEditor(spnNodes));
      val compNodes = Component.wrap(spnNodes)
      def setMaxNodes(n: Int) = swing{ spnNodesMdl.setMaximum(n)}
      def setNodes(n: Int) = swing{ spnNodesMdl.setValue(n)}

      contents += compNodes
    }

    object pnlControl extends GridPanel(1, 2) {
      contents += pnlCommands += pnlSettings
    }

    object MainPanel extends BorderPanel {
      layout(pnlPath) = North
      layout(pnlFiles) = Center
      layout(pnlControl) = South
    }

    def enable(): Unit =
      pnlCommands.enable()

    def disable(): Unit =
      pnlCommands.disable()

    layout(MainPanel) = Center
    layout(pnlStatus) = South
  }

  class PnlLogger extends BorderPanel with SwingUtility {
    val areaConfig: TextArea = new TextArea() {
      editable = false
    }
    def fill(s: String): Unit = swing {
      areaConfig.append(s)
    }
    val caretConfig: DefaultCaret = areaConfig.peer.getCaret.asInstanceOf[DefaultCaret]
    caretConfig.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)

    val scrollArea: ScrollPane = new ScrollPane (areaConfig) {
      horizontalScrollBarPolicy = BarPolicy.Always
      verticalScrollBarPolicy   = BarPolicy.Always
    }
    layout(scrollArea) = Center

  }
}


