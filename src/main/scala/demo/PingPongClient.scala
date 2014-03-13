package demo

import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import com.comsysto.trading.akka.remoting.PingPongActor

object PingPongClient extends App {
  val sys = ActorSystem("PingPong", ConfigFactory.load("application-remoting.conf"))

  {

    sys.actorOf(Props[PingPongActor], "pong")

  }
}
