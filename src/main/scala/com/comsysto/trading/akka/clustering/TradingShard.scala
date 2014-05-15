package com.comsysto.trading.akka.clustering

import akka.actor.{Props, ActorSystem}
import akka.cluster.Cluster
import com.comsysto.trading.akka.OrderRoutingActor
import com.comsysto.trading.provider.SecuritiesProvider
import com.typesafe.config.Config


class TradingShard(val config: Config) {
  this: SecuritiesProvider =>

  val system = ActorSystem("TradingShard", config)
//  val cluster = Cluster(system)

  def init() = {
    val orderRouter = system.actorOf(Props[OrderRoutingActor](new OrderRoutingActor(this.securities)), "orderbooks")
    system.actorOf(Props(classOf[TradingShardManager], orderRouter, this), TradingShardManager.name)

    // -- setup cluster
//    val cluster = Cluster(system)

  }

}
