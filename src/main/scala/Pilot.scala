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


class Pilot extends Actor {
  import Pilots._
  import Plane._

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive: Actor.Receive = {
    case ReadyToGo =>
      // Pilot's parent is Plane
      context.parent ! GiveMeControl

      copilot = context.actorFor("../" + copilotName)

      autopilot = context.actorFor("../Autopilot")

    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

class Copilot extends Actor {
  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters

  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive: Actor.Receive = {
    case ReadyToGo =>
      // controls remain deadLetters
      pilot = context.actorFor("../" + pilotName)
      autopilot = context.actorFor("../Autopilot")
  }
}

class Autopilot extends Actor {
  def receive: Actor.Receive = emptyBehavior
}
