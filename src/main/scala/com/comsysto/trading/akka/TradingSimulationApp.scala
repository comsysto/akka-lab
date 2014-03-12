package com.comsysto.trading.akka

import akka.actor._
import akka.pattern._
import akka.routing.{Broadcast, RoundRobinRouter}
import demo.RoutingStrategies.Receiver.Message
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import com.comsysto.trading.akka.MarketParticipant.Open
import com.comsysto.trading.domain.{Deposit, Security, Depot}

object TradingSimulationApp extends App {
  //Poor mans SecuritiesRepository
  private val securities = List(Security("DE000BAY0017"))

  {
    import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)

    val sys = ActorSystem("TradingSystem")

    //TODO: We want to initialize
    val orderBook = sys.actorOf(Props[OrderBook](null).withRouter(OrderRouter(securities)))

    //TODO: Aggregate these participants below an abstract market?
    val pa = sys.actorOf(Props[MarketParticipant](createMarketParticipant(orderBook, "1200", 1000, 300000000L)), "1200")
    val pb = sys.actorOf(Props[MarketParticipant](createMarketParticipant(orderBook, "1201", 1200, 500000L)), "1201")
    //bank should be more likely to sell securities
    val bank = sys.actorOf(Props[MarketParticipant](createMarketParticipant(orderBook, "1205", 50000000, 5)), "1205")

    pa ! Open
    pb ! Open
    bank ! Open

    //let's suppose a business day takes 20 seconds
    Thread.sleep(20 * 1000)

    for {
      routerSD  <- gracefulStop(orderBook, duration)
      paSD <- gracefulStop(pa, duration)
      pbSD <- gracefulStop(pb, duration)
      bank <- gracefulStop(bank, duration)
    } {
      sys.shutdown()
    }

  }

  def createMarketParticipant(orderRouter : ActorRef, depotAccountNumber : String, depotBalance : Long, depositBalance : Long) = {
    new MarketParticipant(
      orderBook = orderRouter,
      depot = new Depot(depotAccountNumber, securities.head, depotBalance),
      deposit = new Deposit("9" + depotAccountNumber, depositBalance)
    )
  }
}
