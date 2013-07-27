/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 27/07/2013
 */
package zzz.akka.avionics

import akka.actor.{Props, ActorSystem, Actor}
import akka.testkit.{ExtractRoute, ImplicitSender, TestKit}
import zzz.akka.test.ActorSys
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import akka.routing.{Destination, RouterConfig, Route}

class TestRoutee extends Actor {
  def receive: Actor.Receive = Actor.emptyBehavior
}

class TestPassenger extends Actor {
  def receive: Actor.Receive = Actor.emptyBehavior
}

class SectionSpecificAttendantRouterSpec
  extends TestKit(ActorSystem("SectionSpecificAttendantRouterSpec"))
  with ImplicitSender
  with WordSpec
  with BeforeAndAfterAll
  with ShouldMatchers {

  override def afterAll() = system.shutdown()

  def newRouter(): RouterConfig =
    new SectionSpecificAttendantRouter with FlightAttendantProvider {
      override def newFlightAttendant(): Actor = new TestRoutee
    }

  def passengerWithRow(row: Int) =
    system.actorOf(Props[TestPassenger], s"Someone-$row-C")

  val passengers = (1 to 25) map passengerWithRow

  "SectionSpecificAttendantRouter" should {
    "route consistently" in {
      //val router = system.actorOf(Props[TestRoutee].withRouter(newRouter))
      val router = system.actorOf(Props.empty.withRouter(newRouter))

      val route: Route = ExtractRoute(router)
      // pick 10 passengers on row 1 ~ 10(section 0) who will send "hi"
      val routeA: Iterable[Destination] = passengers.slice(0, 10) flatMap { p =>
        route(p, "hi") // : Iterable[Destination]
      }
      // make sure they are all routed to the same attendant
      routeA.tail.forall { dest =>
        dest.recipient == routeA.head.recipient
      } should be (true)

      // pick 3 passengers on row 10(section 0), 11 and 12(section 1).
      val routeAB: Iterable[Destination] = passengers.slice(9, 11) flatMap { p =>
        route(p, "hi") // : Iterable[Destination]
      }
      // make sure 1st doesn't get a route the same as 2ed
      routeAB.head.recipient should not be (routeAB.tail.head.recipient)

      // next 10 passengers on row 11 ~ 20(section 1)
      val routeB: Iterable[Destination] = passengers.slice(10, 20) flatMap { p =>
        route(p, "hi") // : Iterable[Destination]
      }
      // make sure they are all routed to the same attendant
      routeB.tail.forall { dest =>
        dest.recipient == routeB.head.recipient
      } should be (true)


    }
  }
}
