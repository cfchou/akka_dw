/**
 * User: Chifeng.Chou
 * Date: 17/07/13
 * Time: 12:47
 */
package zzz.akka.avionics

import scala.concurrent.duration._

trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

