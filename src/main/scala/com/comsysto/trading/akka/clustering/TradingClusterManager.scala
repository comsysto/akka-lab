package com.comsysto.trading.akka.clustering

import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import akka.actor.{ActorRef, RootActorPath, ActorLogging, Actor}
import com.comsysto.trading.akka.Exchange
import com.comsysto.trading.akka.Exchange.{OpenedResponse, Open, ListSecuritiesResponse, ListSecurities}
import com.comsysto.trading.domain.Security

object TradingClusterManager {
  val name = "tradingClusterManager"

  case class Print(msg: String)
  case class UpdateConfig(globalConfig : Map[Security, ActorRef])

}

class TradingClusterManager extends Actor with ActorLogging {
  import TradingClusterManager._

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
//    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
//      classOf[MemberEvent], classOf[UnreachableMember])
//    log.info(cluster.selfAddress.toString)
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[ClusterDomainEvent])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)


  // ListSecurities -> ListSecuritiesResponse -> Open() -> OpenedResponse -> UpdateConfig to rest

  def receive = following()

  def following(globalConfig : Map[Security, ActorRef] = Map()) : Receive = {
    //TODO: We may get MemberUp messages before we get LeaderChanged -> we might miss some members...
    case LeaderChanged(leader) => {
      if (cluster.selfAddress == leader.get) {
        log.info("New leader is {}", leader)
        //TODO: Determine members
        //TODO: Are we allowed to reuse the global config (where is the old leader?)
        context.become(leading(globalConfig))
      }
    }
    case UpdateConfig(newConfig) => context.become(following(newConfig))
  }


  def leading(globalConfig : Map[Security, ActorRef] = Map(), members : Set[Member] = Set()) : Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      context.actorSelection(RootActorPath(member.address) / "user" / Exchange.name) ! ListSecurities
      context.become(leading(globalConfig, members + member))
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member, previousStatus)
    case LeaderChanged(leader) => {
      if (cluster.selfAddress != leader.get) {
        log.info("Resigning as leader {}. New leader will be {}", cluster.selfAddress, leader.get)
        context.become(following())
      }
    }
    case ListSecuritiesResponse(sec) => {
      val newGlobalConfig = globalConfig ++ sec
      log.info("Opening new exchange")
      sender() ! Open(newGlobalConfig)
      context.become(leading(newGlobalConfig))
    }
    case OpenedResponse => members.foreach { member =>
      log.info("Broadcasting new global config after new exchange has opened")
      context.actorSelection(RootActorPath(member.address) / "user" / TradingClusterManager.name) ! UpdateConfig(globalConfig)
    }


  }
}