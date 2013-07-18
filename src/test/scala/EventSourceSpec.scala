/**
 * User: Chifeng.Chou
 * Date: 16/07/13
 * Time: 14:21
 */
package zzz.akka.avionics

import EventSource.{UnregitsterListener, RegitsterListener}
import akka.actor.{ActorSystem, Actor}
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers

/*
  The test target is ProductionEventSource.
 */

// TestEventSource actor provides EventSource(same as what Altimeter provides)
class TestEventSource extends Actor with ProductionEventSource {
  def receive: Actor.Receive = this.evenSourceReceive
}


class EventSourceSpec
  extends TestKit(ActorSystem("EventSourceSpec"))
  with WordSpec
  with ShouldMatchers
  with BeforeAndAfterAll {

  override protected def afterAll() {
    system.shutdown()
  }

  "EventSoure" should {
    "allow to register a listener" in {
      // TestActorRef:
      //   def apply[T <: Actor](implicit t: ClassTag[T],
      //     system: ActorSystem): TestActorRef[T]
      //   def underlyingActor: T
      val es = TestActorRef[TestEventSource].underlyingActor
      es.receive(RegitsterListener(testActor))  // TestKit.testActor: ActorRef
      es.listeners should contain (testActor)
    }

    "allow to unregister a listener" in {
      val es = TestActorRef[TestEventSource].underlyingActor
      es.receive(RegitsterListener(testActor))  // TestKit.testActor: ActorRef
      es.receive(UnregitsterListener(testActor))  // TestKit.testActor: ActorRef
      es.listeners.size should be (0)

    }

    "send the event to the test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA.receive(RegitsterListener(testActor))  // TestKit.testActor: ActorRef
      testA.underlyingActor.sendEvent("TTTTTTest")
      expectMsg("TTTTTTest") // need to import ImplicitSender
    }
  }

}

