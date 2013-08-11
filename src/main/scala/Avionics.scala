/**
 * User: Chifeng.Chou
 * Date: 15/07/13
 * Time: 17:43
 */
package zzz.akka.avionics

import akka.util.Timeout
import zzz.akka.avionics.Plane._
import scala.concurrent.duration._
import akka.actor.{ActorRef, Props, ActorSystem}
import scala.concurrent.{ExecutionContext, Await}
import akka.pattern.ask

object Avionics {
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  // root of actor hierarchy must be ActorSystem
  // plane is its child
  //val plane = system.actorOf(Props[Plane], "Plane")
  val plane = system.actorOf(Props(Plane()), "Plane")

  val server = system.actorOf(Props(new TelnetServer(plane)), "Telnet")

  import ExecutionContext.Implicits.global
  def main(args: Array[String]) {
    /*
    Await: def result[T](awaitable: Awaitable[T], atMost: Duration): T

     */
    val control = Await.result((plane ? GiveMeControl).mapTo[Controls], 5.seconds).controls
    system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurfaces.StickBack(1f)
    }
    system.scheduler.scheduleOnce(1.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }

    system.scheduler.scheduleOnce(5.seconds) {
      system.shutdown()
    }

  }
}
