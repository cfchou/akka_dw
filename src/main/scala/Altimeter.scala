/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 16:14
 */
package zzz.akka.avionics

import akka.actor.{ActorLogging, Actor}
import scala.concurrent.duration._


object Altimeter {
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
  def apply() = new Altimeter with ProductionEventSource
}

trait AltimeterProvider {
  def newAltimeter: Actor = Altimeter()
}

class Altimeter extends Actor with ActorLogging {
  // Factory must new it with EventSource
  this: EventSource =>

  import Altimeter._

  // dispatcher would be the execution context for scheduler
  implicit val ec = context.dispatcher

  val ceiling = 43000
  val maxRateOfClimb = 5000

  var rateOfClimb: Float = 0f
  var altitude: Double = 0d

  var lastTick = System.currentTimeMillis()
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self,
    Tick)

  case object Tick

  def receive: Actor.Receive = evenSourceReceive orElse {
    case RateChange(amount) =>
      // [-1,1]
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log info(s"Altimeter rateOfClimb to $rateOfClimb.")
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
      lastTick = tick
      sendEvent(AltitudeUpdate(altitude))
  }

  override def postStop() {
    ticker.cancel()
  }
}













