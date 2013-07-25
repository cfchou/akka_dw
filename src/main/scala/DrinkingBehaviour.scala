/**
 * User: Chifeng.Chou
 * Date: 24/07/13
 * Time: 14:32
 */
package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorRef}
import scala.concurrent.duration._

object DrinkingBehaviour {
  // internal msg, blood alcohol level changes
  case class LevelChanged(level: Float)

  // outbound msg
  case object FeelingSober
  case object FeelingTipsy
  case object FeelingLikeZaphod

  def apply(drinker: ActorRef) = new DrinkingBehaviour(drinker)
    with DrinkingResolution
}

trait DrinkingResolution {
  import util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = Random.nextInt(300).seconds
}

trait DrinkingProvider {
  def newDrinkingBehaviour(drinker: ActorRef): Props = Props(DrinkingBehaviour(drinker))
}

class DrinkingBehaviour(drinker: ActorRef) extends Actor {
  this: DrinkingResolution =>
  import DrinkingBehaviour._

  var currentLevel = 0f

  implicit val ec = context.system.dispatcher

  // as times goes, you'll feel more soberer
  val sobering = context.system.scheduler.schedule(initialSobering,
    soberingInterval, self, LevelChanged(-0.0001f))

  override def postStop() {
    sobering.cancel()
  }

  override def preStart() {
    drink()
  }

  def drink() = context.system.scheduler.scheduleOnce(drinkInterval(), self,
    LevelChanged(0.005f))

  def receive: Actor.Receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
      drinker ! (if (currentLevel <= 0.01) {
                  drink()
                  // by the time drinker receives FeelingSober, currentLevel
                  // might become too high.
                  FeelingSober
                } else if (currentLevel <= 0.03) {
                  drink()
                  FeelingTipsy
                } else FeelingLikeZaphod)
  }
}


