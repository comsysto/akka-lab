package com.comsysto.trading.akka

import akka.actor._
import akka.pattern._
import akka.routing.{Broadcast, RoundRobinRouter}
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import com.comsysto.trading.domain.{Deposit, Security, Depot}
import java.util.UUID
import scala.util.Random
import com.typesafe.config.ConfigFactory
import com.comsysto.trading.akka.MarketParticipant.Open

object TradingSimulationApp extends App {

  val random = new Random()
  val config = ConfigFactory.load("simulation.conf")

  //Poor mans SecuritiesRepository
  private val securities = List(Security("DE000BAY0017"))

  val sys = ActorSystem("TradingSystem")

  {
    import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)


    //TODO: We want to initialize
    val orderBook = sys.actorOf(Props[OrderBook](null).withRouter(OrderRouter(securities)))

    val participants = for {
      i <- 1 to config.getInt("com.comsysto.trading.participants.count")
    } yield createMarketParticipant(orderBook, i)

    participants foreach {
      case p: ActorRef => p ! Open
    }

    //let's suppose a business day takes 20 seconds
//    Thread.sleep((60 seconds) toMillis)
//
//    for {
//      routerSD <- gracefulStop(orderBook, duration, Broadcast(PoisonPill))
//      p <- participants
//      pGS <- gracefulStop(p, duration, PoisonPill)
//    } {
//      sys.shutdown()
//    }

  }

  def createMarketParticipant(orderBook: ActorRef, id: Int) : ActorRef = {
    val accountNumber = UUID.randomUUID().toString
    val depot = random(config.getLong("com.comsysto.trading.participants.depot.min"), config.getLong("com.comsysto.trading.participants.depot.max"))
    val deposit = random(config.getLong("com.comsysto.trading.participants.deposit.min"), config.getLong("com.comsysto.trading.participants.deposit.max"))

    sys.actorOf(Props[MarketParticipant](createMarketParticipant(id, orderBook, accountNumber, depot, deposit)), accountNumber)
  }

  def random(min: Long, max: Long): Long = {
    (min + (random.nextDouble() * ((max - min) + 1))).toLong
  }

  def createMarketParticipant(id: Int, orderRouter: ActorRef, depotAccountNumber: String, depotBalance: Long, depositBalance: Long): MarketParticipant = {
    new MarketParticipant(
      id = id,
      orderBook = orderRouter,
      depot = new Depot(depotAccountNumber, securities.head, depotBalance),
      deposit = new Deposit("9" + depotAccountNumber, depositBalance)
    )
  }
}
