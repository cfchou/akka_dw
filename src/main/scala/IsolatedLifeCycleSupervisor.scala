/**
 * User: Chifeng.Chou
 * Date: 18/07/13
 * Time: 17:47
 */
package zzz.akka.avionics

import akka.actor.Actor

object IsolatedLifeCycleSupervisor {
  case object WaitForStart
  case object Started
}


trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  def receive: Actor.Receive = {
    case WaitForStart =>
      sender ! Started
    case m =>
      throw new Exception(s"Don't call ${self.path.name} directly ($m)")
  }

  def childStarter(): Unit

  final override def preStart() = childStarter()

  // don't call preStart()
  final override def postRestart(reason: Throwable) { }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    //postStop()?
  }
}
