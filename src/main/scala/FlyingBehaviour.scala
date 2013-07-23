/**
 * User: Chifeng.Chou
 * Date: 23/07/13
 * Time: 12:06
 */
package zzz.akka.avionics
import scala.concurrent.duration._
import akka.actor.{FSM, Actor, ActorRef}

object FlyingBehaviour {
  import ControlSurfaces._

  // State ====================
  sealed trait State
  case object Idle extends State
  case object Flying extends State
  case object PreparingToFly extends State
  // --------------------------

  case class CourseTarget(altitude: Double, heading: Float, byMillis: Long)
  case class CourseStatus(altitude: Double, heading: Float,
                          headingSinceMS: Long, altitudeSinceMS: Long)

  type Calculator = (CourseTarget, CourseStatus) => Any

  // Data =====================
  sealed trait Data
  case object Uninitialized extends Data
  case class FlightData(controls: ActorRef, elevCalc: Calculator,
                        bankCalc: Calculator, target: CourseTarget,
                        status: CourseStatus) extends Data
  // --------------------------

  // external command
  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis()

  def calcElevator(target: CourseTarget, status: CourseStatus): Any = {
    val alt = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if (alt < 0) StickForward((alt / dur) * -1)
    else StickBack(alt / dur)
  }

  def calcAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{abs, signum}
    val diff = target.heading - status.heading
    val dur = target.byMillis - status.altitudeSinceMS
    val amount = if (abs(diff) < 180) diff
                 else signum(diff) * (abs(diff) - 360f)
    if (amount > 0) StickRight(amount / dur)
    else StickLeft((amount / dur) * -1)
  }


}

class FlyingBehaviour(plane: ActorRef, heading: ActorRef, altimeter: ActorRef)
  extends Actor
  with FSM[FlyingBehaviour.State, FlyingBehaviour.Data] {

  import FlyingBehaviour._
  import Pilots._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._

  // internal msg
  case object Adjust

  /*
  object FSM {
    def using(nextStateDate: D): State[S, D] = {
  }
  trait FSM[S, D] extends Actor with Listeners with ActorLogging {
    type State = FSM.State[S, D]
    type StateFunction = scala.PartialFunction[Event, State]
    type TransitionHandler = PartialFunction[(S, S), Unit]

    case class Event(event: Any, stateData: D)

    def startWith(stateName: S, stateData: D, timeout: Timeout = None): Unit
    def when(stateName: S, stateTimeout: FiniteDuration = null)
      (stateFunction: StateFunction): Unit
    def goto(nextStateName: S): State

    def stay(): State

    def onTransition(transitionHandler: TransitionHandler): Unit

    def transform(func: StateFunction): TransformHelper

    final class TransformHelper(func: StateFunction) {
      def using(andThen2: PartialFunction[State, State]): StateFunction =
        func andThen (andThen2 orElse { case x => x })
        // 1st andThen is a "reversed function composition" combinator: e.g.
        //   f andThen k => k(f(x))
        // as the result we get:
        //   PF[State, State](PF[Event, State](e)): PF[Event, State]
    }
  }
  */

  // This DSL is declarative
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(context.system.deadLetters,
        calcElevator, calcAilerons, target, CourseStatus(-1, -1, 0, 0))
  }

  onTransition {
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }

  when(PreparingToFly, stateTimeout = 5.seconds)(transform {
    // stay PreparingToFly
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(status = d.status.copy(heading = head,
        headingSinceMS = currentMS))

    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(status = d.status.copy(altitude = alt,
        altitudeSinceMS = currentMS))

    case Event(Controls(ctrls), d: FlightData) =>
      stay using d.copy(controls = ctrls)

    // goto Idle, since stateTimeout in `when` is expired
    case Event(StateTimeout, _) =>
      plane ! LostControl
      goto(Idle)
  } using {
    // if all completed, promote to Flying
    case s if preComplete(s.stateData) =>
      s.copy(stateName = Flying)
  })

}


