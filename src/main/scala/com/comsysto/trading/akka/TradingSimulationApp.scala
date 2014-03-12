package com.comsysto.trading.akka

import akka.actor._
import akka.pattern._
import akka.routing.{Broadcast, RoundRobinRouter}
import demo.RoutingStrategies.Receiver.Message
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import com.comsysto.trading.akka.Market.Open
import com.comsysto.trading.domain.{Deposit, Security, Depot}

object TradingSimulationApp extends App {

  {
    import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)

    val sys = ActorSystem("TradingSystem")

    //The one and only - consider introducing a SecurityRepository and retrieving it from there...
    val security = new Security("DE000BAY0017")

    val orderRouter = sys.actorOf(Props[OrderRouter])
    //TODO: Aggregate these participants below an abstract market?
    val pa = sys.actorOf(Props[Market](new Market(orderRouter, new Depot(security, 0), new Deposit(BigDecimal.valueOf(3000000000L)))), "participantA")
    val pb = sys.actorOf(Props[Market](new Market(orderRouter, new Depot(security, 0), new Deposit(BigDecimal.valueOf(5000000L)))), "participantB")
    //bank should be more likely to sell securities
    val bank = sys.actorOf(Props[Market](new Market(orderRouter, new Depot(security, 5000000), new Deposit(BigDecimal.valueOf(1000)))), "bank")

    pa ! Open
    pb ! Open
    bank ! Open

    //let's suppose a business day takes 20 seconds
    Thread.sleep(20 * 1000)

    for {
      routerSD  <- gracefulStop(orderRouter, duration)
      paSD <- gracefulStop(pa, duration)
      pbSD <- gracefulStop(pb, duration)
      bank <- gracefulStop(bank, duration)
    } {
      sys.shutdown()
    }

  }
}
