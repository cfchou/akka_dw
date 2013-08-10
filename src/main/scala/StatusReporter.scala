/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 10/08/2013
 */
package zzz.akka.avionics

import akka.actor.Actor

object StatusReporter {
  // indicating that status should be reported
  case object ReportStatus

  sealed trait Status
  case object StatusOK extends Status
  case object StatusNotGreat extends Status
  case object StatusBad extends Status
}


trait StatusReporter {
  this: Actor =>

  import StatusReporter._

  def currentStatus: Status

  def statusReceive: Receive = {
    case ReportStatus =>
      sender ! currentStatus
  }
}





