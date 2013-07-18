/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 12:49
 */
package zzz.akka.avionics

import akka.actor.Actor

object FlightAttendant {
  case class GetDrink(drinkkname: String)
  case class Drink(drinkname: String)
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS: Int = 300000
  }
}


class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  implicit val ec = context.dispatcher // for scheduler

  def receive: Actor.Receive = {
    case GetDrink(drinkname) =>
      // schedule to serve
      // def scheduleOnce(FiniteDuration, ActorRef, Any)
      // even sender is a function, sender is frozen at call site(call-by-value).
      context.system.scheduler.scheduleOnce(responseDuration, sender,
        Drink(drinkname))
  }
}


