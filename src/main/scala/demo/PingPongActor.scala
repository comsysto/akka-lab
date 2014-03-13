package demo

import akka.actor.{ActorLogging, Actor}

object PingPongActor {
  case class Ping(message : String, counter : Long, timestamp : Long)
  case class Pong(message : String, counter : Long, timestamp : Long)
}

class PingPongActor extends Actor with ActorLogging {
  import demo.PingPongActor.{Pong, Ping}

  var counter = 2l
  def next = {
    counter = counter + 1
    counter
  }

  def receive = {
    case Ping(m, c, ts) => {
      if (c % 9 == 0) log.info(s"Ping: $c : $m")
      sender ! Pong(m, next, ts)
      next
      sender ! Ping(m, next, System.nanoTime())
      next
    }

    case Pong(m, c, ts) => {
      val roundTripTime = System.nanoTime() - ts
      if (c % 9 == 0) log.info(s"Pong: $c : $m with round trip time $roundTripTime ns.")
      sender ! Ping(m, next, System.nanoTime())
      next
      sender ! Pong(m, next, ts)
      next
    }

  }

}
