/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 25/07/2013
 */
package zzz.akka.avionics

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import zzz.akka.avionics.Passenger._

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin: FiniteDuration = 0.millis
  override val requestUpper: FiniteDuration = 2.millis
}


class PassengerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpec
  with ShouldMatchers {
  import akka.event.Logging.Info
  import akka.testkit.TestProbe

  var seatNumber = 9
  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(
      Props(new Passenger(testActor) with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      // def subscribe(subscriber: ActorRef, channel: Class[_]): Boolean
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatBelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString should include (" fastening seatbel")
      }
    }

  }
}
