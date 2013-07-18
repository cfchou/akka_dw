/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 15:13
 */
package zzz.akka.avionics

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.config.ConfigFactory

object TestFlightAttendant {
  // factory
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS: Int = 1
  }
}

class FlightAttendantSpec
  extends TestKit(ActorSystem("FlightAttendantSpec",
    ConfigFactory.parseString("akka.scheduler.tick-duration = 1ms")))
  with ImplicitSender
  with WordSpec
  with ShouldMatchers {

  import FlightAttendant._

  "FlightAttendant" should {
     "get a drink when asked" in {
       val a = TestActorRef(Props(TestFlightAttendant()))
       a ! GetDrink("martini")
       expectMsg(Drink("martini"))
     }
  }



}
