package com.comsysto.trading.akka

import akka.actor._
import akka.util.Timeout
import akka.util.Timeout._
import com.comsysto.trading.domain.{Deposit, Depot}
import java.util.UUID
import scala.util.Random
import com.comsysto.trading.akka.MarketParticipant.Open
import com.comsysto.trading.provider.{SimpleSecuritiesProvider, ConfigProvider}

object TradingSimulationApp extends App with ConfigProvider with SimpleSecuritiesProvider {

  val random = new Random()

  val sys = ActorSystem("TradingSystem")

  {
//    import scala.concurrent.duration._
//    val duration = 3000
    implicit val timeout: Timeout = 3000l

    //TODO: We want to initialize
    val orderBook = sys.actorOf(Props[OrderBook](null).withRouter(new OrderRouter with SimpleSecuritiesProvider), "orderbooks")

    val participants = for {
      i <- 1 to config.getInt("participants.count")
    } yield createMarketParticipant(orderBook, i)

    participants foreach {
      case p: ActorRef => p ! Open
    }

//    let's suppose a business day takes 20 seconds
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
    val depot = random(config.getLong("participants.depot.min"), config.getLong("participants.depot.max"))
    val deposit = random(config.getLong("participants.deposit.min"), config.getLong("participants.deposit.max"))

    sys.actorOf(Props[MarketParticipant](createMarketParticipant(id, orderBook, accountNumber, depot, deposit)), accountNumber)
  }

  def random(min: Long, max: Long): Long = {
    (min + (random.nextDouble() * ((max - min) + 1))).toLong
  }

  def createMarketParticipant(id: Int, orderRouter: ActorRef, depotAccountNumber: String, depotBalance: Long, depositBalance: Long): MarketParticipant = {
    new MarketParticipant(
      id = id,
      orderBook = orderRouter,
      depot = new Depot(depotAccountNumber, Random.shuffle(securities).head, depotBalance),
      deposit = new Deposit("9" + depotAccountNumber, depositBalance)
    )
  }
}
