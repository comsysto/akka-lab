package demo

import akka.actor.{ActorLogging, Actor}

object PingPongActor {
  case class Ping(message : String, counter : Long)
  case class Pong(message : String, counter : Long)
}

class PingPongActor extends Actor with ActorLogging {

  def receive = {
    case Ping(m, c) => {
      log.info(s"Ping: $c : $m")
      sender ! Pong(m, c + 1)
    }

    case Pong(m, c) => {
      log.info(s"Pong: $c : $m")
      sender ! Ping(m, c + 1)
    }

  }
}
