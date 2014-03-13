package demo

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import PingPongActor._

/**
 * Created by sturmm on 13.03.14.
 */
object PingPongServer extends App {

  val akkaConf = ConfigFactory.load("application-remoting.conf").withOnlyPath("akka")
  val serverConf = ConfigFactory.load("application-remoting.conf").getConfig("server")

  val conf = serverConf.withFallback(akkaConf)

  val system = ActorSystem("PingPong", conf)

  val actor = system.actorOf(Props[PingPongActor], "ping")

  system.actorSelection("akka.tcp://PingPong@192.168.2.50:2552/user/pong").tell(Ping("Hello!", 1, System.nanoTime()), actor)

}
