/**
 * User: Chifeng.Chou
 * Date: 18/07/13
 * Time: 17:47
 */
package zzz.akka.avionics

import akka.actor.{ActorKilledException, ActorInitializationException, SupervisorStrategy, Actor}
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._

object IsolatedLifeCycleSupervisor {
  case object WaitForStart
  case object Started
}


trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  def receive: Actor.Receive = {
    case WaitForStart =>
      // Tell them I'm started/restarted.
      sender ! Started
    case m =>
      throw new Exception(s"Don't call ${self.path.name} directly ($m)")
  }

  // Create children.
  def childStarter(): Unit

  // Only create children when preStart(NOT doing it in preRestart).
  final override def preStart() = childStarter()

  // Don't call preStart(), so no children is re-created.
  final override def postRestart(reason: Throwable) { }

  // Don't stop children.
  override def preRestart(reason: Throwable, message: Option[Any]) { }
}


// no limit if maxNrRetries < 0
// no window if withinTimeRange == Duration.Inf
abstract class IsolatedResumeSupervisor(maxNrRetries: Int = -1,
                                        withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>
  override val supervisorStrategy: SupervisorStrategy =
    makeStrategy(maxNrRetries, withinTimeRange) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Resume  // Other Exceptions cause it to Resume.
      case _ => Escalate
    }
}

abstract class IsolatedStopSupervisor(maxNrRetries: Int = -1,
                                      withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>
  override val supervisorStrategy: SupervisorStrategy =
    makeStrategy(maxNrRetries, withinTimeRange) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Stop  // Other Exceptions cause it to Stop.
      case _ => Escalate
    }
}





