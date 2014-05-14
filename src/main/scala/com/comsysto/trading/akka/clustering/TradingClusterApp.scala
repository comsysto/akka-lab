package com.comsysto.trading.akka.clustering

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.ActorSystem
import akka.actor.Props
import com.comsysto.trading.akka.Exchange
import com.comsysto.trading.provider.SecuritiesProvider
import com.comsysto.trading.domain.Security
import akka.cluster.Cluster
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
      node =>
      val port = node._1
      val nodeId = node._2
      // Override the configuration of the port
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
        withFallback(clusterConfig)

      // Create an Akka system
      val system = ActorSystem("ClusterSystem", config)
      val cluster = Cluster(system)
      // Create an actor that handles cluster domain events
      cluster.registerOnMemberUp {
        system.actorOf(Props[TradingClusterManager], name = "clusterManager")
      }

      val secProvider = new ClusterSecuritiesProvider(clusterConfig, s"exchange.$nodeId.securities")
      val exchange = system.actorOf(Props[Exchange](new Exchange(secProvider.securities)), Exchange.name)
    }
  }

}

class ClusterSecuritiesProvider(val config : Config, val configPath: String) extends SecuritiesProvider {
  override def securities: List[Security] = (for {
    name <- config.getStringList(configPath)
  } yield Security(name)).toList

}

