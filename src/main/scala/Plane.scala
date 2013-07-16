/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 16:48
 */
package dwMain

import akka.actor.{Props, Actor, ActorLogging}
import dwMain.EventSource.RegitsterListener

object Plane {
  case object GiveMeControl
}

class Plane extends Actor with ActorLogging {
  import Altimeter._
  import Plane._

  // child of this actor
  val altimeter = context.actorOf(Props[Altimeter], "Altimeter")

  /*
  Props: def apply(creator: ⇒ Actor): Props
  Returns a Props that has default values except for "creator" which will be a
  function that creates an instance using the supplied thunk
   */
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)),
    "ControlSurfaces")

  override def preStart() {
    altimeter ! RegitsterListener
  }

  def receive: Actor.Receive = {
    case GiveMeControl =>
      log info("Demand Plane to give control.")
      // give sender the ControlSurface ActorRef
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is $altitude.")

  }
}
