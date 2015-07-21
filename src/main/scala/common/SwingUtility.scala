package common

import rx.lang.scala.{Observable, Subscription}

import scala.swing.event.{ButtonClicked, MouseClicked}
import scala.swing.{Button, Table}

trait SwingUtility {
  def swing(body: =>Unit) = {
    val r = new Runnable { def run() = body }
    javax.swing.SwingUtilities.invokeLater(r)
  }

  implicit class ButtonOps(val self: Button) {
    def clicks = Observable.create[Boolean] { obs =>
      self.reactions += {
        case ButtonClicked(_) => obs.onNext(true)
      }
      Subscription()
    }
  }

  implicit class TableOps(val self: Table) {
    def rowDoubleClicks = Observable.create[Int] { sub =>
      self.reactions += {
        case e: MouseClicked =>
          if(e.peer.getClickCount == 2)
            sub.onNext(self.peer.getSelectedRow)
      }
      Subscription()
    }
  }
}
