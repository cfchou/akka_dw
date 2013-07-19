/**
 * User: Chifeng.Chou
 * Date: 19/07/13
 * Time: 10:19
 */
package zzz.akka.avionics

import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.{AllForOneStrategy, OneForOneStrategy, SupervisorStrategy}

// SupervisionStrategy factory traits that can be mixed in.

trait SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)
                  (decider: Decider): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)
                  (decider: SupervisorStrategy.Decider): SupervisorStrategy = {
    OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
  }
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)
                  (decider: SupervisorStrategy.Decider): SupervisorStrategy = {
    AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
  }
}





