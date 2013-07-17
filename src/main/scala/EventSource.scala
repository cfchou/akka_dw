/**
 * User: Chifeng.Chou
 * Date: 16/07/13
 * Time: 10:46
 */
package dwMain
import akka.actor.{Actor, ActorRef}

object EventSource {
  case class RegitsterListener(listener: ActorRef)
  case class UnregitsterListener(listener: ActorRef)
}

trait EventSource {  this: Actor =>
  def evenSourceReceive: Actor.Receive
  def sendEvent[T](event: T): Unit
}

trait ProductionEventSource extends EventSource { this: Actor =>
  import EventSource._

  var listeners = Vector.empty[ActorRef]

  val evenSourceReceive: Actor.Receive = {
    case RegitsterListener(listener) =>
      listeners = if (listeners.contains(listener)) listeners
      else listeners :+ listener
    case UnregitsterListener(listener) =>
      listeners = listeners.filter(_ != listener)
  }

  def sendEvent[T](event: T): Unit = {
    listeners.map(_ ! event)
  }

}
