/**
 * User: Chifeng.Chou
 * Date: 16/07/13
 * Time: 15:58
 */
package dwTest

import akka.testkit.{TestActorRef, TestLatch, TestKit}
import akka.actor.{Props, Actor, ActorSystem}
import dwMain.Altimeter.{AltitudeUpdate, RateChanging}
import dwMain.{Altimeter, EventSource}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.Await
import scala.concurrent.duration._

/*
AltimeterSpec is meant to test Altimeter's messages only, therefore the
EventSource part is replaced by a mock object.
 */
class AltimeterSpec
  extends TestKit(ActorSystem("AltimeterSpec"))
  with WordSpec
  with ShouldMatchers
  with BeforeAndAfterAll {

  override protected def afterAll() {
    system.shutdown()
  }

  class Helper {
    object EventSourceSpy {
      // class TestLatch(count: Int = 1)(implicit system: ActorSystem)
      //   extends Awaitable[Unit]
      val latch = TestLatch(1)
    }
    // a mock object
    trait EventSourceSpy extends EventSource { this: Actor =>

      def sendEvent[T](event: T) {
        EventSourceSpy.latch.countDown()
      }

      // don't care about register/unregister listeners
      def evenSourceReceive: Actor.Receive = Actor.emptyBehavior
    }

    // factory of Altimeter
    def slicedAltimeter = new Altimeter with EventSourceSpy

    def actor(): (TestActorRef[Altimeter], Altimeter) = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }

  "Altimeter" should {
    "record rate of climb changes" in new Helper {
      val (_, al) = actor()
      al.receive(RateChanging(1f))
      al.rateOfClimb should be (al.maxRateOfClimb)
    }

    "keep rate of climb changes within bounds" in new Helper {
      val (_, al) = actor()
      al.receive(RateChanging(2f))
      al.rateOfClimb should be (al.maxRateOfClimb)
    }

    // real, not mock
    "calculating altitude changes" in {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegitsterListener(testActor)
      ref ! RateChanging(1f)

      // def fishForMessage(...)(f: PartialFunction[Any, Boolean]): Any
      // wait for messages, won't come back unless timeout or the partial
      // function returns true
      fishForMessage() {
        case AltitudeUpdate(altitude) if (altitude == 0f) => false
        case AltitudeUpdate(altitude) => true
      }
    }

    "send events" in new Helper {
      val (ref, _) = actor()
      // Await: def ready[T](awaitable: Awaitable[T], atMost: Duration)
      // : awaitable.type
      // Await the "completed" state of an `Awaitable`.
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be (true)
    }


  }

}
