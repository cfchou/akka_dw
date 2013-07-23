/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 16:43
 */
package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}

object ControlSurfaces {
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)

  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)
}

class ControlSurfaces(val plane: ActorRef, val altimeter: ActorRef,
                      val heading: ActorRef)
  extends Actor {
  import ControlSurfaces._
  import Altimeter._
  import HeadingIndicator._

  def receive: Actor.Receive = controlledBy(context.system.deadLetters)

  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if sender == somePilot =>
      altimeter ! RateChange(amount)
    case StickForward(amount) if sender == somePilot =>
      altimeter ! RateChange(-1 * amount)
    case StickRight(amount) if sender == somePilot =>
      altimeter ! BankChange(amount)
    case StickLeft(amount) if sender == somePilot =>
      altimeter ! BankChange(-1 * amount)

    // Plane signals that control is handed over to a new entity
    case HasControl(entity) if sender == plane =>
      context.become(controlledBy(entity))
  }
}
