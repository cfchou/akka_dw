/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 17:17
 */
package zzz.akka.test
import zzz.akka.avionics
import akka.actor.{Props, ActorSystem}
import zzz.akka.avionics.{LeadFlightAttendant, AttendantCreationPolicy}

object FlightAttendantPathChecker {
  def main(args: Array[String]) = {
    val sys = ActorSystem("PlaneSimulation")
    val lead = sys.actorOf(Props(LeadFlightAttendant()),
      sys.settings.config.getString(
        "zzz.akka.avionics.flightcrew.leadAttendantName"))
    Thread.sleep(2000)
    sys.shutdown()
  }
}
