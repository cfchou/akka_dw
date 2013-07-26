/**
 * User: Chifeng.Chou
 * Date: 26/07/13
 * Time: 12:49
 */
package zzz.akka.avionics

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem, Actor, ActorRef}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import zzz.akka.avionics.PassengerSupervisor.{PassengerBroadcaster, GetPassengerBroadcaster}
import scala.concurrent.duration._

object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString(
    """
      |zzz.akka.avionics.passengers = [
      |[ "Kelly Franqui", "23", "A" ],
      |[ "Tyrone Dotts", "23", "B" ],
      |[ "Malinda Class", "23", "C" ],
      |[ "Kenya Jolicoeur", "24", "A" ],
      |[ "Christian Piche", "24", "B" ]
      |]
    """.stripMargin)
}

trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive: Actor.Receive = {
        case m => callButton ! m
      }
    }
}

class PassengerSupervisorSpec
  extends TestKit(ActorSystem("PassengerSupervisorSpec"
    , PassengerSupervisorSpec.config))
  with ImplicitSender
  with WordSpec
  with BeforeAndAfterAll
  with ShouldMatchers {
  import PassengerSupervisorSpec._

  override protected def afterAll() {
    system.shutdown()
  }

  "PassengerSupervisor" should {
    "work" in {
      val a = system.actorOf(
        Props(new PassengerSupervisor(testActor) with TestPassengerProvider))

      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
      val sayhi = "hi there"
      broadcaster ! sayhi
      // 5 test passengers response "hi there" to callButton, which is our
      // testActor
      expectMsg(sayhi)
      expectMsg(sayhi)
      expectMsg(sayhi)
      expectMsg(sayhi)
      expectMsg(sayhi)
      // and no further responses
      expectNoMsg(100.millis)

      a ! GetPassengerBroadcaster
      // pattern matching only success if it matches existing local val
      // broadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }
}









