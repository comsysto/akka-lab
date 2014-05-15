package com.comsysto.trading.akka.clustering

import com.typesafe.config.{Config, ConfigFactory}
import com.comsysto.trading.provider.SecuritiesProvider
import com.comsysto.trading.domain.Security
import scala.collection.JavaConversions._

object TradingClusterApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq(("2551", "cluster0"), ("2552", "cluster1")))
    //else
    //  startup(args.)
  }

  def startup(nodes: Seq[Pair[String, String]]): Unit = {
    val clusterConfig: Config = ConfigFactory.load("application-trading-cluster.conf")

    nodes foreach {
      case (port, nodeId) =>

      // -- Override the configuration of the port
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
        withFallback(clusterConfig)

      trait ClusterSecuritiesProvider extends SecuritiesProvider {
        override def securities: List[Security] = (for {
          name <- config.getStringList(s"exchange.$nodeId.securities")
        } yield Security(name)).toList

      }

      // -- setup domain
      val shard = new TradingShard(config) with ClusterSecuritiesProvider

      shard.init()
    }
  }

}

