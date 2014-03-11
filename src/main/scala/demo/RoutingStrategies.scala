package demo

import akka.actor._
import akka.pattern._
import akka.routing.{Broadcast, RoundRobinRouter}
import demo.RoutingStrategies.Reciever.Message
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global

object RoutingStrategies extends App {

  object Reciever {
    case class Message(msg: String)
  }

  class Reciever(timeout: Long) extends Actor with ActorLogging{

    def this() = this(1000)

    import demo.RoutingStrategies.Reciever._

    override def receive = fastRecieve

    def fastRecieve: Receive = {
      case msg @ Message(m)=> {
        log.info(m)

        context.become(slowRecieve)
      }
    }

    def slowRecieve: Receive = {
      case msg @ Message(m) => {
        Thread.sleep(timeout)

        log.info(s"Slow: $m")

        context.become(fastRecieve)
      }
    }
  }



  {
    import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)

    val sys = ActorSystem("Routing")

    val single = sys.actorOf(Props[Reciever](new Reciever(2.seconds.toMillis)), "single")
    val router = sys.actorOf(Props[Reciever].withRouter(RoundRobinRouter(nrOfInstances = 10)), "router")

    // NOTE: Be aware that there is no guarantee for Message order, when mixing up ActorRefs and ActorSelections
    sys.actorSelection("user/single") ! Message("Another Message by path!")

    single ! Message("Hello You!")
    single ! Message("Hello Block!")

    router ! Message("Hello Anybody!")
    router ! Broadcast(Message("Hello World!"))

    for {
      routerSD  <- gracefulStop(router, duration)
      singleSD <- gracefulStop(single, duration)
    } {
      sys.shutdown()
    }

  }
}
