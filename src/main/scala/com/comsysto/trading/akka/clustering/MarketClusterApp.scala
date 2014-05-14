package com.comsysto.trading.akka.clustering

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.Cluster
import clustering.SimpleClusterListener.Print
import com.comsysto.trading.provider.{SimpleSecuritiesProvider, ConfigProvider}
import scala.util.Random
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import java.util.UUID
import com.comsysto.trading.akka.MarketParticipant
import com.comsysto.trading.domain.{Deposit, Depot}
import com.comsysto.trading.akka.MarketParticipant.Open

/**
 * Created by sturmm on 13.05.14.
 */
object MarketClusterApp extends App with SimpleSecuritiesProvider {

  val random = new Random()

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2553").
    withFallback(ConfigFactory.load("application-trading-cluster.conf").getConfig("market"))

  println(config.toString)

  val sys = ActorSystem("ClusterSystem", config)

  val a1: Address = Address("akka.tcp", "ClusterSystem", "127.0.0.1", 2551)
  val a2: Address = Address("akka.tcp", "ClusterSystem", "127.0.0.1", 2552)

  val cluster: Cluster = Cluster(sys)
  cluster.joinSeedNodes(a1 :: a2 :: Nil)

  cluster.registerOnMemberUp(init())


  def init() = {
    implicit val timeout = Timeout.apply(3, TimeUnit.SECONDS)

    //TODO: Will not work
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