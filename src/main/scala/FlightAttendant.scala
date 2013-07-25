/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 12:49
 */
package zzz.akka.avionics

import akka.actor.{Cancellable, ActorRef, Actor}

object FlightAttendant {
  case class GetDrink(drinkkname: String)
  case class Drink(drinkname: String)

  case class Assist(passenger: ActorRef)
  case object Busy_?
  case object Yes
  case object No

  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS: Int = 300000
  }
}


class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  implicit val ec = context.dispatcher // for scheduler

  // internal msg signals that a delivery can take place
  case class DeliverDrink(drinkname: String)
  // stores scheduleDelivery, could be canceled
  var pendingDelivery: Option[Cancellable] = None

  def scheduleDelivery(drinkname: String): Cancellable = {
    context.system.scheduler.scheduleOnce(responseDuration, self,
      DeliverDrink(drinkname))
  }

  def assistInjuredPassenger: Receive = {
    case Assist(passenger) =>
      pendingDelivery map { _.cancel() }
      pendingDelivery = None
      passenger ! Drink("Magic")
  }

  def handleDrinkRequests: Receive = {
    case GetDrink(drinkname) =>
      pendingDelivery = Some(scheduleDelivery(drinkname))
      // orElse is short-circut
      context.become(assistInjuredPassenger orElse handleSpecificPerson(sender))
    case Busy_? =>
      sender ! No
  }

  // we're serving someone
  def handleSpecificPerson(person: ActorRef): Receive = {
    // the person we're serving has a new request
    case GetDrink(drinkname) if sender == person =>
      pendingDelivery map { _.cancel() }
      pendingDelivery = Some(scheduleDelivery(drinkname))

    // ScheduleDelivery signals us to deliver drink now
    case DeliverDrink(drinkname) =>
      person ! Drink(drinkname)
      pendingDelivery = None
      context.become(assistInjuredPassenger orElse handleDrinkRequests)

    // got request from another person, forward to the lead attendant
    case m: GetDrink =>
      context.parent forward(m)

    case Busy_? =>
      sender ! Yes
  }

  def receive = assistInjuredPassenger orElse handleDrinkRequests
}


