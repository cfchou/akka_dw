/**
 * User: Chifeng.Chou
 * Date: 18/07/13
 * Time: 11:43
 */
package zzz.akka.avionics

import akka.actor.{Props, Terminated, ActorRef, Actor}
import akka.actor.FSM.SubscribeTransitionCallBack
import zzz.akka.avionics.DrinkingBehaviour.{FeelingLikeZaphod, FeelingTipsy, FeelingSober}

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
               heading: ActorRef, altimeter: ActorRef): Actor = {
    new Pilot(plane, autopilot, controls, heading, altimeter)
      with DrinkingProvider
      with FlyingProvider
  }

  def newCopilot(plane: ActorRef, autopilot: ActorRef,
                 altimeter: ActorRef): Actor = {
    new Copilot(plane, autopilot, altimeter)
  }

  def newAutopilot(plane: ActorRef): Actor = new Autopilot(plane)
}

class Pilot(plane: ActorRef, autopilot: ActorRef,
            var controls: ActorRef, // mutable, might give away controls
            heading: ActorRef, altimeter: ActorRef)
  extends Actor {
  this: DrinkingProvider with FlyingProvider =>

  import Pilots._
  import Plane._
  import FlyingBehaviour._
  import akka.actor.FSM.Transition
  import akka.actor.FSM.CurrentState

  //var controls: ActorRef = context.system.deadLetters
  //var copilot: ActorRef = context.system.deadLetters
  //var autopilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  /*
  def receive: Actor.Receive = {
    case Controls(controlSurfaces) =>
      // regain controls
      controls = controlSurfaces
  }
  */
  def receive: Actor.Receive = bootstrap

  def setCourse(flyer: ActorRef) = {
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
  }

  override def preStart() {
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading, altimeter),
      "FlyingBehaviour")
  }

  def bootstrap: Receive = {
    case ReadyToGo =>
      val copilot = context.actorFor("../" + copilotName)
      val flyer = context.actorFor("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // already zaphod
  }

  // pilot doesn't do anything at all
  def idle: Receive = {
    case _ =>
  }

  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  override def unhandled(message: Any) {
    message match {
      // case class Transition[S](fsmRef: ActorRef, from: S, to: S)
      case Transition(_, _, Flying) =>
        setCourse(sender)
      case Transition(_, _, Idle) =>
        context.become(idle)
      // intercept the following two, preventing them from entering the log
      case Transition(_, _, _) =>
      case CurrentState(_, _) =>
      //
      case m => super.unhandled(m)
    }
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
