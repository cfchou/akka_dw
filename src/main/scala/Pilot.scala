/**
 * User: Chifeng.Chou
 * Date: 18/07/13
 * Time: 11:43
 */
package zzz.akka.avionics

import akka.actor.{Terminated, ActorRef, Actor}

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

  def newAutopilot(plane: ActorRef): Actor = new Autopilot(plane)
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
  import Plane._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive: Actor.Receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      context.watch(pilot)
    case Terminated(_) =>
      plane ! GiveMeControl
    case Controls(controlSurfaces) =>
      // gain controls(pilot died)
      controls = controlSurfaces
  }
}

class Autopilot(plane: ActorRef) extends Actor {
  import Pilots._
  import Plane._
  var controls: ActorRef = context.system.deadLetters

  def receive: Actor.Receive = {
    case ReadyToGo =>
      plane ! RequestCopilot
    case CopilotReference(copilot) =>
      context.watch(copilot)
    case Terminated(_) =>
      plane ! GiveMeControl
    case Controls(controlSurfaces) =>
      // gain controls(pilot died, copilot died too)
      controls = controlSurfaces
  }
}
