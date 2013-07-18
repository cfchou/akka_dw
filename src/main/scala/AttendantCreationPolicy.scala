/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 16:42
 */
package zzz.akka.avionics

import akka.actor.Actor

trait AttendantCreationPolicy {
  val numberOfAttendants: Int = 8
  def createAttendant: Actor = FlightAttendant()
}





