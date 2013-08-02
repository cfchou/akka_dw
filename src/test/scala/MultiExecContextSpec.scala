/**
 * User: Chifeng.Chou
 * Date: 30/07/13
 * Time: 11:04
 */
package zzz.akka.investigation

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.duration._

class MultiExecContextSpec extends WordSpec with ShouldMatchers {
  import scala.math.BigInt

  lazy val fibs: Stream[BigInt] = BigInt(0) #:: BigInt(1) #::
    fibs.zip(fibs.tail).map { n => n._1 + n._2 }

  "Future" should {
    import java.util.concurrent.Executors
    import scala.concurrent.{ExecutionContext, Future, Await}

    "calculate fibonacci numbers" in {
      // create EC from plain java execution service
      val execService = Executors.newCachedThreadPool()
      implicit val execContext = ExecutionContext
        .fromExecutorService(execService)

      val futureFib = Future { fibs.drop(99).head } // (implicit execContext)

      val fib = Await.result(futureFib, 1.second)
      fib should be (BigInt("218922995834555169026"))

      execContext.shutdown() // otherwise this thread will never die
    }


    import scala.util.{Failure, Success}

    "calculate factors for a fib" in {
      val execService = Executors.newCachedThreadPool()
      implicit val execContext = ExecutionContext
        .fromExecutorService(execService)

      // def andThen[U](pf: PartialFunction[Try[T], U])
      //   (implicit executor: ExecutionContext): Future[T]
      val futureFac = Future { fibs.drop(28).head } map { factorize _ } andThen {
        case Success(Tuple2(fib, factors)) =>
          println(s"Factors for $fib are ${factors.mkString(", ")}")
        case Failure(e) =>
          println("Something went wrong - " + e)
      }
    }
  }

  def factorize(num: BigInt): Tuple2[BigInt, Seq[Int]] = {
    import scala.math._
    (num, (1 to floor(sqrt(num.toDouble)).toInt).filter { 0 == num % _ })
  }

}
