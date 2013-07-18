/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 16:48
 */
package zzz.akka.avionics

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import EventSource.RegitsterListener
import zzz.akka.avionics.Pilots.ReadyToGo

object Plane {
  case object GiveMeControl
  case class Controls(val controls: ActorRef)
}

class Plane extends Actor with ActorLogging {
  import Altimeter._
  import Plane._

  // child of this actor
  //val altimeter = context.actorOf(Props[Altimeter], "Altimeter")

  // Props: def apply(creator: => Actor): Props
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")

  /*
  Props: def apply(creator: => Actor): Props
  Returns a Props that has default values except for "creator" which will be a
  function that creates an instance using the supplied thunk
   */
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)),
    "ControlSurfaces")

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot],
    config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[Copilot],
    config.getString(s"$cfgstr.copilotName"))
  val autopilot = context.actorOf(Props[Autopilot], "Autopilot")

  // lead flight attendant
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()),
    config.getString(s"$cfgstr.leadAttendantName"))


  override def preStart() {
    altimeter ! RegitsterListener(self)
    List(pilot, copilot) map { _ ! ReadyToGo }
  }

  def receive: Actor.Receive = {
    case GiveMeControl =>
      log info("Demand Plane to give control.")
      // give sender the ControlSurface ActorRef
      sender ! Controls(controls)
    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is $altitude.")

  }
}
