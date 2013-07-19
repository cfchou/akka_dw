/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 16:48
 */
package zzz.akka.avionics

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import zzz.akka.avionics.IsolatedLifeCycleSupervisor.WaitForStart
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask

object Plane {
  case object GiveMeControl
  case class Controls(val controls: ActorRef)

  def apply(): Plane =
    new Plane with AltimeterProvider
      with PilotProvider
      with LeadFlightAttendantProvider
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
        with PilotProvider
        with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._


  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val leadAttendantName = config.getString(s"$cfgstr.leadAttendantName")

  override def preStart() {
    import EventSource.RegitsterListener
    import Pilots.ReadyToGo

    startEquipment()
    startPeople() // call startEquipment first

    actorForControls("Altimeter") ! RegitsterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive: Actor.Receive = {
    case GiveMeControl =>
      log info("Demand Plane to give control.")
      // give sender the ControlSurface ActorRef
      sender ! Controls(actorForControls("ControlSurfaces"))
    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is $altitude.")
  }

  implicit val askTimeout = Timeout(1.second)

  def startEquipment(): Unit = {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        // Create children.
        def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(newAutopilot), "Autopilot")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  // call startEquipment first
  def startPeople(): Unit = {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")


    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        // Create children.
        def childStarter() {
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
        }
      }), "Pilots")

    /* Lead flight attendant. It is supervised directly by the plane. It
    follows default life-cycle and strategy. E.g.

    1. When getting "Restart" from the parent, it restarts all its children in
    preRestart/postRestart.

    2. When getting exception from its children, restart them on one-to-one basis,
    which could run infinite times if it keeps failing.
    */
    context.actorOf(Props(newLeadFlightAttendant), leadAttendantName)

    // Only wait for pilots, since newLeadFlightAttendant might run infinitely.
    Await.result(people ? WaitForStart, 1.second)
  }

  def actorForControls(name: String): ActorRef = {
    context.actorFor("Equipment/" + name)
  }

  def actorForPilots(name: String): ActorRef = {
    context.actorFor("Pilots/" + name)
  }

}
