package demo

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

object PingPongClient extends App {

  val akkaConf = ConfigFactory.load("application-remoting.conf").withOnlyPath("akka")
  val clientConf = ConfigFactory.load("application-remoting.conf").getConfig("client")

  val conf = clientConf.withFallback(akkaConf)

  val sys = ActorSystem("PingPong", conf)

  {

    sys.actorOf(Props[PingPongActor], "pong")

  }
}
