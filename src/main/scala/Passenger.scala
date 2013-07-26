/**
 * User: Chifeng.Chou
 * Date: 25/07/13
 * Time: 16:57
 */
package zzz.akka.avionics
import scala.concurrent.duration._
import akka.actor.{ActorLogging, Actor, ActorRef}

object Passenger {
  // to tell passengers
  case object FastenSeatBelts
  case object UnfastenSeatBelts

  // regex to extract Name-Row-Seat
  val SeatAssignment = """([\w\s_]+)-(\d+)-([A-Z])""".r
}

trait DrinkRequestProbability {
  val askThreshold = 0.9f

  // minimum time between requests
  val requestMin = 20.minutes

  // time between requests = ??% * requestUpper + requestMin
  val requestUpper = 30.minutes

  def randomishTime(): FiniteDuration = {
    requestMin + util.Random.nextInt(requestUpper.toMillis.toInt).minutes
  }
}

trait PassengerProvider {
  def newPassenger(callButton: ActorRef): Actor =
    new Passenger(callButton) with DrinkRequestProbability
}

class Passenger(callButton: ActorRef) extends Actor with ActorLogging {
  this: DrinkRequestProbability =>

  import Passenger._
  import FlightAttendant.{GetDrink, Drink}
  import collection.JavaConverters._

  val r = scala.util.Random
  case object CallForDrink

  // Path(Uri)es have spaces replaced by '_'s. Convert them back.
  val SeatAssignment(myname, _, _) = self.path.name.replaceAllLiterally("_",
    " ")

  val drinks = context.system.settings.config.getStringList(
    "zzz.akka.avionics.drinks").asScala.toIndexedSeq

  implicit val ec = context.system.dispatcher
  val scheduler = context.system.scheduler

  override def preStart() {
    self ! CallForDrink
  }

  // decide whether or not I want a drink
  def maybeSendDrinkRequest(): Unit = {
    if (r.nextFloat() > askThreshold) {
      val drinkname = drinks(r.nextInt(drinks.length))
      callButton ! GetDrink(drinkname)
    }
    // next time I will decide again
    scheduler.scheduleOnce(randomishTime(), self, CallForDrink)
  }

  def receive: Actor.Receive = {
    case CallForDrink =>
      maybeSendDrinkRequest()
    case Drink(drinkname) =>
      log info s"$myname received a $drinkname - Yum"
    case FastenSeatBelts =>
      log info s"$myname fastening seatbelt"
    case UnfastenSeatBelts =>
      log info s"$myname unfastening seatbelt"
  }
}
