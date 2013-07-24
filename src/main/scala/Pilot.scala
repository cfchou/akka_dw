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

  import FlyingBehaviour._
  import ControlSurfaces._


  val tipsyCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    // making the msg biased since he's tipsy
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m  // is it necessary?
    }
  }

  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m  // is it necessary?
    }
  }

  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m  // is it necessary?
    }
  }

  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m  // is it necessary?
    }
  }
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
  this: DrinkingProvider with FlyingProvider =>

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
