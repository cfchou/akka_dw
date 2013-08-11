/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 11/08/2013
 */
package zzz.akka.avionics

import akka.actor._
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString

class PlaneForTelnet extends Actor {

  import HeadingIndicator._
  import Altimeter._

  def receive: Actor.Receive = {
    case GetCurrentAltitude =>
      sender ! CurrentAltitude(52500f)
    case GetCurrentHeading =>
      sender ! CurrentHeading(233.4f)
  }
}

class TelnetServerSpec
  extends TestKit(ActorSystem("TelnetServerSpec"))
  with ImplicitSender
  with WordSpec
  with ShouldMatchers {

  "TelnetServer" should {
    "work" in {
      val p = system.actorOf(Props[PlaneForTelnet])
      val s = system.actorOf(Props(new TelnetServer(p)))

      val socket = IOManager(system).connect("localhost", 31733)
      expectMsgType[IO.Connected]
      expectMsgType[IO.Read]

      socket.write(ByteString("heading"))
      expectMsgPF() {
        case IO.Read(_, bytes) =>
          val result = TelnetServer.ascii(bytes)
          result should include ("233.40")
      }

      socket.write(ByteString("altitude"))
      expectMsgPF() {
        case IO.Read(_, bytes) =>
          val result = TelnetServer.ascii(bytes)
          result should include ("52500.00")
      }
      socket.close()
    }
  }
}
