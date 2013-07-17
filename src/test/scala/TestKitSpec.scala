/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 14/07/2013
 */
package dwTest
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{ParallelTestExecution, BeforeAndAfterAll, WordSpec, fixture}
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.atomic.AtomicInteger


object MyActor {
  case object Ping
  case object Pong
}

class MyActor extends Actor {
  import MyActor._
  def receive: Actor.Receive = {
    case Ping => {}
    case Pong => {}
  }
}

import dwTest.MyActor.{Pong, Ping}

class TestKitSpec(actorSystem: ActorSystem)
  extends TestKit(actorSystem)
  with WordSpec
  // with ClassicMatchers
  with ShouldMatchers
  with BeforeAndAfterAll

class MyActorSpecBad
  extends TestKitSpec(ActorSystem("MyActorSpecBad"))
  with ParallelTestExecution {

  override protected def afterAll() {
    //super.afterAll()
    system.shutdown()
  }

  def makeActor(): ActorRef =  system.actorOf(Props[MyActor], "MyActor")

  "MyActor" should {
    "throw if constructed with a wrong name" in {
      evaluating {
        val a = system.actorOf(Props[MyActor])
      } should produce[Exception]
    }

    "construct without exception" in {
      val a = makeActor()
    }

    "respond with a Pong to a Ping" in {
      val a = makeActor()
      a ! Ping
      expectMsg(Pong)
    }
  }
}

// ==============================================
object ActorSys {
  val uniqueId = new AtomicInteger(0)
}

/*
 NoArg only appears in unreleased scalatest-2.0.M6.
 http://www.artima.com/docs-scalatest-2.0.M6-SNAP21/index.html#org.scalatest.fixture.NoArg
 https://github.com/scalatest/scalatest/tree/master/src/main/scala/org/scalatest/fixture

 We create a NoArg equivalent here
 */

trait NoArg extends DelayedInit with (() => Unit) {
  private var body: () => Unit = _
  final def delayedInit(x: => Unit) { body = (() => x) }
  def apply() { body() }

  // Suite.styleName: String, therefore trying to mix NoArg in a Suite causes
  // compile error.
  final val styleName: Int = 0
}

class ActorSys(name: String)
  extends TestKit(ActorSystem(name))
  with ImplicitSender
  with NoArg {

  def this() = this("TestSystem%05d".format(ActorSys.uniqueId.getAndIncrement))

  def shutdown(): Unit = system.shutdown()

  override def apply() {
    try super.apply()
    finally system.shutdown()
  }
}

class MyActorSpec
  extends WordSpec
  with ShouldMatchers
  with ParallelTestExecution {

  def makeActor(sys: ActorSystem): ActorRef =  sys.actorOf(Props[MyActor], "MyActor")

  "MyActor" should {
    "throw if constructed with a wrong name" in new ActorSys {
      evaluating {
        val a = system.actorOf(Props[MyActor])
      } should produce[Exception]
    }

    "construct without exception" in new ActorSys {
      val a = makeActor(system)
    }

    "respond with a Pong to a Ping" in new ActorSys {
      val a = makeActor(system)
      a ! Ping
      expectMsg(Pong)
    }
  }
}


