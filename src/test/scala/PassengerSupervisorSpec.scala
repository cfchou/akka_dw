/**
 * User: Chifeng.Chou
 * Date: 26/07/13
 * Time: 12:49
 */
package zzz.akka.avionics

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import zzz.akka.avionics.PassengerSupervisor.PassengerBroadcaster
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
    new Actor with ActorLogging {
      def receive: Actor.Receive = {
        case m =>
          log.info(s"${self.path.name} received ${m.toString} from ${sender.path.name}, send to ${callButton.path.name}")
          callButton ! m
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
      val p = TestProbe()
      val a = system.actorOf(
        Props(new PassengerSupervisor(p.ref) with TestPassengerProvider),
        "testPS")

      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
      val sayhi = "hi there"
      broadcaster ! sayhi
      // 5 test passengers response "hi there" to callButton, which is our
      // testProbe
      p.expectMsg(sayhi)
      p.expectMsg(sayhi)
      p.expectMsg(sayhi)
      p.expectMsg(sayhi)
      p.expectMsg(sayhi)
      // and no further responses
      p.expectNoMsg(100.millis)

      a ! GetPassengerBroadcaster
      // pattern matching only success if it matches existing local val
      // broadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }
}









