/**
 * User: Chifeng.Chou
 * Date: 23/07/13
 * Time: 10:45
 */
package zzz.akka.avionics

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._

object HeadingIndicator {
  // How fast we're changing direction
  case class BankChange(amount: Float)

  case class CurrentHeading(amount: Float)
  case object GetCurrentHeading

  // Event published to indicate where we're headed
  case class HeadingUpdate(heading: Float)
  def apply() = new HeadingIndicator with ProductionEventSource
}

trait HeadingIndicatorProvider {
  def newHeadingIndicator: Actor = HeadingIndicator()
}

trait HeadingIndicator
  extends Actor
  with ActorLogging
  with StatusReporter {
  this: EventSource =>

  import HeadingIndicator._
  import StatusReporter._

  def currentStatus: Status = StatusOK

  // internal msg to recalculate the heading
  case object Tick

  val maxDegPerSec = 5

  implicit val ec = context.system.dispatcher
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self,
    Tick)

  var lastTick = 0f

  var rateOfBank = 0f

  var heading = 0f

  def headingIndicatorReceive: Receive = {
    // keeps the rate of change within [-1, 1]
    case BankChange(amount) =>
      rateOfBank = amount.min(1.0f).max(-1.0f)

    case Tick =>
      val tick = System.currentTimeMillis()
      val timeDelta = (tick - lastTick) / 1000f // in seconds
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      sendEvent(HeadingUpdate(heading))

    case GetCurrentHeading =>
      sender ! CurrentHeading(heading)
  }

  def receive = statusReceive orElse
    evenSourceReceive orElse
    headingIndicatorReceive

  override def postStop() = ticker.cancel()
}
