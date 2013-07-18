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
}

class ControlSurfaces(val altimeter: ActorRef) extends Actor {
  import ControlSurfaces._
  import Altimeter._

  def receive: Actor.Receive = {
    case StickBack(amount) =>
      altimeter ! RateChanging(amount)
    case StickForward(amount) =>
      altimeter ! RateChanging(-1 * amount)
  }
}
