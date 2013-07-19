/**
 * User: Chifeng.Chou
 * Date: 18/07/13
 * Time: 11:43
 */
package zzz.akka.avionics

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.emptyBehavior

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef,
               altimeter: ActorRef): Actor = {
    new Pilot(plane, autopilot, controls, altimeter)
  }

  def newCopilot(plane: ActorRef, autopilot: ActorRef,
                 altimeter: ActorRef): Actor = {
    new Copilot(plane, autopilot, altimeter)
  }

  def newAutopilot: Actor = new Autopilot
}

class Pilot(plane: ActorRef, autopilot: ActorRef,
            var controls: ActorRef, // mutable, might give away controls
            altimeter: ActorRef)
  extends Actor {
  import Pilots._
  import Plane._

  //var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  //var autopilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive: Actor.Receive = {
    case ReadyToGo =>
      copilot = context.actorFor("../" + copilotName)

    case Controls(controlSurfaces) =>
      // regain controls
      controls = controlSurfaces
  }
}

class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef)
  extends Actor {
  import Pilots._

  //var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters

  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive: Actor.Receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
  }
}

class Autopilot extends Actor {
  def receive: Actor.Receive = emptyBehavior
}
