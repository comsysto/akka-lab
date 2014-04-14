package com.comsysto.trading.akka.remoting

import com.comsysto.trading.provider.{SimpleSecuritiesProvider, ConfigProvider}
import scala.util.Random
import akka.actor.{ActorRef, Props, ActorSystem}
import com.comsysto.trading.akka.{MarketParticipant, OrderRouter, OrderBook}
import java.util.UUID
import com.comsysto.trading.domain.{Security, Deposit, Depot}
import com.comsysto.trading.akka.MarketParticipant.{TradeSecurity, Open}
import com.typesafe.config.ConfigFactory
import akka.util.Timeout

/**
  * Created by sturmm on 13.03.14.
  */
object RemoteClientApp extends App with ConfigProvider with SimpleSecuritiesProvider {

  val random = new Random()

  val sys = ActorSystem("TradingSystem", ConfigFactory.load("application-remoting.conf"))

  {
    import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)

    //TODO
    //val orderBook = sys.actorSelection("akka.tcp://TradingSystem@192.168.2.220:2552/user/orderbook").resolveOne().value.get.get
    val orderBook = sys.actorFor("akka.tcp://TradingSystem@192.168.2.220:2552/user/orderbook")

    val participants = for {
      i <- 1 to config.getInt("participants.count")
    } yield createMarketParticipant(orderBook, i)

    participants foreach {
      case p: ActorRef => p ! Open //; p ! TradeSecurity
    }
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
