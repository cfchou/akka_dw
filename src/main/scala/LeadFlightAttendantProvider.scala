/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 16:44
 */
package zzz.akka.avionics

import akka.actor.Actor

trait LeadFlightAttendantProvider {
  def newLeadFlightAttendant: Actor = LeadFlightAttendant()
}
