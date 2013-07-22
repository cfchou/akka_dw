/**
 * User: Chifeng.Chou
 * Date: 22/07/13
 * Time: 12:07
 */
package zzz.akka.avionics

import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import IsolatedLifeCycleSupervisor.WaitForStart
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import zzz.akka.avionics.Pilots.ReadyToGo
import akka.actor.Actor.emptyBehavior


object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
    zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
    zzz.akka.avionics.flightcrew.pilotName = "$pilotName"
    """
}

class PilotsSpec
  extends TestKit(ActorSystem("PilotsSpec",
    ConfigFactory.parseString(PilotsSpec.configStr)))
  with ImplicitSender
  with WordSpec
  with ShouldMatchers {

  import PilotsSpec._
  import Plane._

  def nilActor: ActorRef = TestProbe().ref
  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def pilotsReadyToGo(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)
    val a = system.actorOf(Props(
      new IsolatedStopSupervisor with OneForOneStrategyFactory {
        // Create children.
        def childStarter() {
          context.actorOf(Props[FakePilot], pilotName)
          // testActor sits in for the Plane
          context.actorOf(Props(new Copilot(testActor, nilActor, nilActor)),
            copilotName)
        }
      }), "TestPilots")
    Await.result(a ? WaitForStart, 3.seconds)
    system.actorFor(copilotPath) ! ReadyToGo
    a
  }

  "Copilot" should {
    "take control when pilot dies" in {
      pilotsReadyToGo()
      // Messages that exist ahead of the PoisonPill will be processed
      system.actorFor(pilotPath) ! PoisonPill
      expectMsg(GiveMeControl)
      lastSender should be (system.actorFor(copilotPath))
    }
  }
}

// will be supervised by IsolatedStopSupervisor
class FakePilot extends Actor {
  def receive: Actor.Receive = emptyBehavior
}
