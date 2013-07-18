/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 16:45
 */
package zzz.akka.avionics

import akka.actor.{Props, ActorRef, Actor}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy {
    override val numberOfAttendants: Int = 10
  }
}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>
  import LeadFlightAttendant._

  override def preStart() {
    import collection.JavaConverters._
    // val config: Config(Java)
    val attendantNames = context.system.settings.config.getStringList(
      "zzz.akka.avionics.flightcrew.attendantNames").asScala

    (attendantNames take numberOfAttendants).map { name =>
      // implicit val context: ActorContext
      // this actor's context
      context.actorOf(Props(createAttendant), name) // child of this actor
    }
  }

  def randomAttendant(): ActorRef = {
    // def children: immutable.Iterable[ActorRef]
    // children in this ActorContext
    context.children.take(util.Random.nextInt(numberOfAttendants - 1) + 1).last
  }

  def receive: Actor.Receive = {
    case GetFlightAttendant =>
      sender ! Attendant(randomAttendant())
    case m =>
      // def forward(message: Any)(implicit context: ActorContext)
      // = tell(message, context.sender)
      randomAttendant() forward m
  }
}






