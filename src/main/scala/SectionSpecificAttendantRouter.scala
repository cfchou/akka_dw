/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 27/07/2013
 */
package zzz.akka.avionics

import akka.routing.{Destination, RouteeProvider, RouterConfig, Route}
import akka.actor.{Props, SupervisorStrategy}
import akka.dispatch.Dispatchers

class SectionSpecificAttendantRouter extends RouterConfig {
  this: FlightAttendantProvider =>

  def routerDispatcher: String = Dispatchers.DefaultDispatcherId

  def supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.defaultStrategy

  // invokes decision making code
  def createRoute(routeeProvider: RouteeProvider): Route = {
    val attendants = (1 to 5) map { n =>
      routeeProvider.context.actorOf(Props(newFlightAttendant)
        , "Attendant-" + n)
    }

    routeeProvider.registerRoutees(attendants)

    // type Route = PartialFunction[(ActorRef, Any), Iterable[Destination]]
    // case class Destination(sender: ActorRef, recipient: ActorRef)
    // calculates Destinations based on the name of sender
    {
      case (sender, _) =>
        import Passenger.SeatAssignment // regex to extract Name-Row-Seat
        val SeatAssignment(_, row, _) = sender.path.name
        // 10 rows as 1 section
        List(Destination(sender, attendants(row.toInt / 11)))
    }
  }


}
