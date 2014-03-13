package demo

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

object PingPongClient extends App {
  val sys = ActorSystem("PingPong", ConfigFactory.load("application-remoting.conf"))

  {

    sys.actorOf(Props[PingPongActor], "pong")

  }
}
