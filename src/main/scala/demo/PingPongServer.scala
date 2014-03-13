package demo

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import PingPongActor._
import demo.PingPongActor

/**
 * Created by sturmm on 13.03.14.
 */
object PingPongServer extends App {

  val system = ActorSystem("PingPong", ConfigFactory.load("application-remoting.conf"))

  val actor = system.actorOf(Props[PingPongActor], "ping")

  system.actorSelection("akka.tcp://PingPong@192.168.2.50:2552/user/pong").tell(Ping("Hello!", 1), actor)

}
