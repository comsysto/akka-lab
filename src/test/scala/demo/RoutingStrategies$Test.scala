package demo

import akka.testkit._
import akka.actor.ActorSystem
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import org.scalatest.Matchers
import demo.RoutingStrategies.Receiver
import demo.RoutingStrategies.Receiver.Message
import akka.event.Logging.Info

class RoutingStrategiesSpec extends TestKit(ActorSystem("EventSourceSpec"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "Receiver" should {
    "log its output" in {
      val receiver = TestActorRef[Receiver].underlyingActor
      val loggingProbe = TestProbe()

      system.eventStream.subscribe(loggingProbe.ref, classOf[Info])
      receiver.receive(Message("Hello World"))
      loggingProbe.expectMsgPF() {
        case Info(_, _, m) => m.toString should include ("Hello World")
      }
    }

    "toggle slow mode and fast mode" in {
      val receiver = TestActorRef[Receiver]
      val loggingProbe = TestProbe()

      system.eventStream.subscribe(loggingProbe.ref, classOf[Info])
      //Fast mode...
      receiver ! Message("Hello Fast World")
      loggingProbe.expectMsgPF() {
        case Info(_, _, m) => {
          m.toString should include ("Hello Fast World")
          m.toString should not include ("Slow:")
        }
      }
      //Slow mode...
      receiver ! Message("Hello Slow World")
      loggingProbe.expectMsgPF() {
        case Info(_, _, m) => m.toString should include ("Slow: Hello Slow World")
      }
    }
  }
}