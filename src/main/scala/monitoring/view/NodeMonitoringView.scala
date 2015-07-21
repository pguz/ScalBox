package monitoring.view

import javax.swing.table.DefaultTableModel

import common.SwingUtility

import scala.swing.BorderPanel.Position._
import scala.swing._

trait NodeMonitoringView extends SwingUtility {

  def view(s: String) = new View(s)

  class View(val frameTitle: String) extends MainFrame {
    title = frameTitle

    object gridMain extends GridPanel(1, 2) {
      val filePane = PaneFiles
      val pnlConfig = PnlConfig
      contents += filePane += pnlConfig
    }

    contents = new BorderPanel {
      layout(gridMain) = Center
    }
  }

  object PaneFiles extends ScrollPane {
    val columnNames =
      Array[AnyRef]("Filename", "Size", "Date modified")
    val fileTable = new Table {
      showGrid = true
      model = new DefaultTableModel(columnNames, 0) {
        override def isCellEditable(r: Int, c: Int) = false
      }
      selection.intervalMode = Table.IntervalMode.Single
    }
    contents = fileTable

    def updatePane(files: List[(String, Long, String)]) = swing {
      fileTable.model match {
        case d: DefaultTableModel =>
          d.setRowCount(0)
          files.foreach(f => {
            val array: Array[AnyRef] = Array[AnyRef](f._1, f._2.toString, f._3)
            d.addRow(array)
          })
      }
    }
  }

  object PnlConfig extends BorderPanel {
    val areaConfig: TextArea = new TextArea() {
      editable = false
    }
    def fill(s: String): Unit = swing {
      areaConfig.text = s
    }
    layout(areaConfig) = Center
  }
}
