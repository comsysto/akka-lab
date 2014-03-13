package com.comsysto.trading.akka.remoting

import akka.actor.{ActorLogging, ActorRef, Actor}

object PingPongActor {
  case class Ping(message : String, counter : Long)
  case class Pong(message : String, counter : Long)
}

class PingPongActor extends Actor with ActorLogging {
  import com.comsysto.trading.akka.remoting.PingPongActor._

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
