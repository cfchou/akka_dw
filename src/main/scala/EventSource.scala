/**
 * User: Chifeng.Chou
 * Date: 16/07/13
 * Time: 10:46
 */
package zzz.akka.avionics
import akka.actor.{Actor, ActorRef}

object EventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource {  this: Actor =>
  def evenSourceReceive: Actor.Receive
  def sendEvent[T](event: T): Unit
}

trait ProductionEventSource extends EventSource { this: Actor =>
  import EventSource._

  var listeners = Vector.empty[ActorRef]

  val evenSourceReceive: Actor.Receive = {
    case RegisterListener(listener) =>
      listeners = if (listeners.contains(listener)) listeners
      else listeners :+ listener
    case UnregisterListener(listener) =>
      listeners = listeners.filter(_ != listener)
  }

  def sendEvent[T](event: T): Unit = {
    listeners.map(_ ! event)
  }

}
