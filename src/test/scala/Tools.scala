import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{FunSuiteLike, Matchers}

/**
 * Created by galvanize on 6/8/15.
 */
trait Tools {
  this: TestKit with FunSuiteLike with Matchers with ImplicitSender =>
}
