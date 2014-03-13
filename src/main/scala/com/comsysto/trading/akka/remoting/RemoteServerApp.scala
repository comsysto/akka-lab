package com.comsysto.trading.akka.remoting

import akka.actor.{Props, ActorSystem}
import com.comsysto.trading.akka.OrderBook
import com.comsysto.trading.domain.Security
import com.comsysto.trading.algorithm.{AverageMarketPriceCalculator, SimpleTradeMatcher}
import com.typesafe.config.ConfigFactory

/**
 * Created by sturmm on 13.03.14.
 */
object RemoteServerApp extends App {

  val system = ActorSystem("TradingSystem", ConfigFactory.load("application-remoting.conf"))

  val orderbook = system.actorOf(Props[OrderBook](new OrderBook(security = Security("Daimler")) with SimpleTradeMatcher with AverageMarketPriceCalculator))

}
