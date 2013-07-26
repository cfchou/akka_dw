/**
 * User: Chifeng.Chou
 * Date: 26/07/13
 * Time: 10:22
 */
package zzz.akka.avionics

import akka.actor._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.{Stop, Resume, Escalate}
import akka.routing.BroadcastRouter
import scala.collection.immutable

object PassengerSupervisor {
  // Allow someone to request the BroadcastRouter
  case object GetPassengerBroadcaster

  // Returns the BroadcastRouter
  case class PassengerBroadcaster(broadcaster: ActorRef)

  def apply(callButton: ActorRef) =
    new PassengerSupervisor(callButton) with PassengerProvider
}


class PassengerSupervisor(callButton: ActorRef) extends Actor with ActorLogging {
  this: PassengerProvider =>

  import PassengerSupervisor._

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  // internal msgs
  case class GetChildren(forSomeone: ActorRef)
  case class Children(children: immutable.Iterable[ActorRef], childrenFor: ActorRef)

  override def preStart() {
    // an intermediate supervisor
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config

      override def supervisorStrategy: SupervisorStrategy =
        OneForOneStrategy() {
          case _: ActorKilledException => Escalate
          case _: ActorInitializationException => Escalate
          case _ => Stop
        }

      override def preStart() {
        import collection.JavaConverters._
        import com.typesafe.config.ConfigList

        val passengers = config.getList("zzz.akka.avionics.passengers")
        passengers.asScala.map { nameWithSeat =>
          // unwrapped to List<String>, turn it to List[String]
          // mkString to Name-Row-Seat, replace any space in Name with "_"
          val id = nameWithSeat.asInstanceOf[ConfigList].unwrapped().asScala
            .mkString("-").replaceAllLiterally(" ", "_")

          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      def receive: Actor.Receive = {
        case GetChildren(forSomeone) =>
          log.info(s"${self.path.name} received GetChildren from ${sender.path.name}")
          sender ! Children(context.children, forSomeone)
      }
    }), "StopSupervisor")
  }

  def noRouter: Actor.Receive = {
    case GetPassengerBroadcaster =>
      val passengers = context.actorFor("StopSupervisor")
      passengers ! GetChildren(sender)

    case Children(passengers, destinedFor) =>
      log.info(s"noRouter receive ${passengers.toString} and ${destinedFor.path.name}")
      val t = BroadcastRouter(passengers)
      //val t0 = Props()
      val t0 = Props.empty
      val t1 = t0.withRouter(t)
      val router = context.actorOf(t1, "Passengers")
      /*
      val router = context.actorOf(
        Props().withRouter(BroadcastRouter(passengers)), "Passengers")
      */
      destinedFor ! PassengerBroadcaster(router)
      context.become(yesRouter(router))
  }

  def yesRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster =>
      sender ! PassengerBroadcaster(router)

    // Might be many GetPassengerBroadcaster in noRouter state.
    // First Children response flips the state, and still subsequent Children
    // responses need to be handled.
    case Children(_, destinedFor) =>
      destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}
