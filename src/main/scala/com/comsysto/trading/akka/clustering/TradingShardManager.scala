package com.comsysto.trading.akka.clustering

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor._
import com.comsysto.trading.provider.SecuritiesProvider
import com.comsysto.trading.akka.clustering.TradingShardManager._
import akka.routing.FromConfig
import com.comsysto.trading.domain.Security
import com.comsysto.trading.akka.clustering.TradingShardManager.UpdateGlobalConfig
import akka.cluster.ClusterEvent.LeaderChanged
import com.comsysto.trading.akka.OrderRoutingActor.UpdateConfig

object TradingShardManager {
  val name = "tradingShardManager"

  case object ShardConfigRequest
  case class ShardConfig(securities: Map[Security, ActorRef])

  case class NotConfigured(reply: Any)

  case class UpdateGlobalConfig(globalConfig : Map[Security, ActorRef])

}

class TradingShardManager(val orderRouter: ActorRef, val securitiesProvider: SecuritiesProvider) extends Actor with ActorLogging {

  var leading = false

  val cluster = Cluster(context.system)

  val shardManagerRouter = context.system.actorOf(FromConfig.props(), s"${TradingShardManager.name}Router")

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[ClusterDomainEvent])

    cluster.sendCurrentClusterState(self)
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = handleLeaderChanged orElse follower

  def handleLeaderChanged: Receive = {
    case LeaderChanged(newLeader) => newLeader map { address =>
      if(address.equals(cluster.selfAddress)) {
        context.become(handleLeaderChanged orElse leader(Map.empty) orElse follower)
        shardManagerRouter ! ShardConfigRequest
        log.info(s"Leader changed and I'm the new one!")
      }
      else {
        context.become(handleLeaderChanged orElse follower)
        log.info(s"Leader changed but it's somebody else....")
      }
    }
  }

  def leader(config: Map[Security, ActorRef]): Receive = {
    case ShardConfig(c) =>
      log.info(s"Got config from ${sender()}. Updating config.")

      val newConfig = config ++ c
      context.become(handleLeaderChanged orElse leader(newConfig) orElse follower)

      shardManagerRouter ! UpdateGlobalConfig(newConfig)
    case MemberRemoved(m, ms) =>
      log.info(s"Member $m has left cluster. Clean up config.")
  }

  def follower: Receive = {
    case UpdateGlobalConfig(conf) =>
      orderRouter ! UpdateConfig(conf)
      log.info(s"Got new global config $conf.")
    case ShardConfigRequest =>
      sender ! ShardConfig(securitiesProvider.securities map (_ -> orderRouter) toMap)
      log.info(s"Responded local config to ShardConfigRequest.")
  }

}