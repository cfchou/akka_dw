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
import akka.routing.FromConfig

object Plane {
  case object GiveMeControl
  case class Controls(val controls: ActorRef)

  case object RequestCopilot
  case class CopilotReference(val copilot: ActorRef)

  case object LostControl

  def apply(): Plane =
    new Plane with AltimeterProvider
      with PilotProvider
      with HeadingIndicatorProvider
      with LeadFlightAttendantProvider
      with FlightAttendantProvider
}

class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
        with PilotProvider
        with HeadingIndicatorProvider
        with LeadFlightAttendantProvider
        with FlightAttendantProvider =>

  import Altimeter._
  import Plane._


  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val leadAttendantName = config.getString(s"$cfgstr.leadAttendantName")

  override def preStart() {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    startEquipment()
    startPeople() // call startEquipment first

    actorForControls("Altimeter") ! RegisterListener(self)
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
    case RequestCopilot =>
      sender ! CopilotReference(actorForControls(copilotName))
    case LostControl =>
      actorForPilots("Autopilot") ! Controls(actorForControls(
        "ControlSurfaces"))
  }

  implicit val askTimeout = Timeout(1.second)

  def startEquipment(): Unit = {
    val plane = self
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        // Create children.
        def childStarter() {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          val head = context.actorOf(Props(newHeadingIndicator), "Heading")
          context.actorOf(Props(newAutopilot(self)), "Autopilot")
          context.actorOf(Props(new ControlSurfaces(plane, alt, head))
            , "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  // call startEquipment first
  def startPeople(): Unit = {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val heading = actorForControls("Heading")
    val altimeter = actorForControls("Altimeter")

    /* Lead flight attendant. It is supervised directly by the plane. It
    follows default life-cycle and strategy. E.g.

    1. When getting "Restart" from the parent, it restarts all its children in
    preRestart/postRestart.

    2. When getting exception from its children, restart them on one-to-one basis,
    which could run infinite times if it keeps failing.
    */
    //context.actorOf(Props(newLeadFlightAttendant), leadAttendantName)

    /*
    As mentioned in
    http://www.artima.com/forums/flat.jsp?forum=289&thread=349624
    -- This isn't a good example.

    PassengerSupervisor's BroadcastRouter which accepts many routees
    which will be broadcast by BroadcastRouter.

    But here we will have a pool of lead attendants by config. One is randomly
    chosen(RandomRouter) to randomly choose(our code) an attendant.
     */
    val leadAttendant = context.actorOf(
      Props(newFlightAttendant).withRouter(FromConfig())
      , "FlightAttendantRouter")

    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        // Create children.
        def childStarter() {
          context.actorOf(Props(newPilot(plane, autopilot, controls, heading
            , altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter))
            , copilotName)
          // leadAttendant acts like the CallButton for passengers
          context.actorOf(Props(PassengerSupervisor(leadAttendant))
            , "PassengerSupervisor")
        }
      }), "Pilots")

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
