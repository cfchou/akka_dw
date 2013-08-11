/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 10/08/2013
 */
package zzz.akka.avionics

import akka.actor._
import akka.util.{ByteString, Timeout}
import scala.concurrent.duration._
import scala.collection.mutable.Map
import akka.pattern.ask
import scala.util.{Success, Failure}

object TelnetServer {
  implicit val askTimeout = Timeout(1.second)
  val welcome =
    """
      |Welcome to the airplane
      |------------------------
      |
      |commands: heading, altitude
      |
    """.stripMargin

  def ascii(bytes: ByteString): String = {
    bytes.utf8String.trim
  }

  case class NewMessage(msg: String)

  class SubServer(socket: IO.SocketHandle, plane: ActorRef) extends Actor {

    import HeadingIndicator._
    import Altimeter._

    implicit val ec = context.dispatcher

    def headStr(head: Float): ByteString =
      ByteString(f"heading is: $head%3.2f degrees\n\n> ")

    def altStr(alt: Double): ByteString =
      ByteString(f"altitude is: $alt%5.2f feet\n\n> ")

    def unknown(str: String): ByteString =
      ByteString(f"current $str is: unknown\n\n> ")

    def handleHeading() = {
      (plane ? GetCurrentHeading).mapTo[CurrentHeading].onComplete {
        case Success(CurrentHeading(heading)) =>
          socket.write(headStr(heading))
        case Failure(_) =>
          socket.write(unknown("heading"))
      }
    }

    def handleAltitude() = {
      (plane ? GetCurrentAltitude).mapTo[CurrentAltitude].onComplete {
        case Success(CurrentAltitude(alt)) =>
          socket.write(altStr(alt))
        case Failure(_) =>
          socket.write(unknown("altitude"))
      }
    }

    def receive: Actor.Receive = {
      case NewMessage(msg) =>
        msg match {
          case "heading" =>
            handleHeading()
          case "altitude" =>
            handleAltitude()
          case m =>
            socket.write(ByteString("What?\n\n>"))
        }
    }
  }
}

class TelnetServer(plane: ActorRef) extends Actor with ActorLogging {

  import TelnetServer._

  // IO will be moved to akka.io package
  // Handle: An immutable handle to a Java NIO Channel
  val subservers = Map.empty[IO.Handle, ActorRef]

  val serverSocket = IOManager(context.system).listen("0.0.0.0", 31733)

  def receive: Actor.Receive = {
    case IO.Listening(server, address) =>
      log.info("server on port {}", address)

    case IO.NewClient(server) =>
      log.info("new connection")
      val socket = server.accept()
      socket.write(ByteString(welcome))
      // one sub-server(actor) for one client
      subservers += (socket
        -> context.actorOf(Props(new SubServer(socket, plane))))

    case IO.Read(socket, bytes) =>
      val cmd = ascii(bytes)
      subservers(socket) ! NewMessage(cmd)

    case IO.Closed(socket, cause) =>
      context.stop(subservers(socket))
      subservers -= socket
  }
}
