package com.comsysto.trading.akka.clustering

import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import akka.actor._
import com.comsysto.trading.akka.Exchange
import com.comsysto.trading.akka.Exchange.{OpenedResponse, Open, ListSecuritiesResponse, ListSecurities}
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Security
import com.comsysto.trading.akka.Exchange.Open
import akka.actor.RootActorPath
import com.comsysto.trading.akka.Exchange.ListSecuritiesResponse
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.LeaderChanged
import akka.cluster.ClusterEvent.UnreachableMember

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
        context.become(leading(globalConfig, Set(leader.get)))

        exchange(cluster.selfAddress) ! ListSecurities
      }
    }
    case UpdateConfig(newConfig) => context.become(following(newConfig))
  }


  def leading(globalConfig : Map[Security, ActorRef], members : Set[Address]) : Receive = {
    case MemberUp(member) => {
      log.info("Member is Up: [address: {}, roles: {}]", member.address, member.getRoles)
      if (member.hasRole("exchange")) {
        exchange(member.address) ! ListSecurities
        context.become(leading(globalConfig, members + member.address))
      }
    }
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
      context.become(leading(newGlobalConfig, members))
    }
    case OpenedResponse => {
      log.info("Broadcasting new global config after new exchange has opened")
      members.foreach { address =>
        context.actorSelection(RootActorPath(address) / "user" / TradingClusterManager.name) ! UpdateConfig(globalConfig)
      }
    }

  }

  def exchange(address: Address) : ActorSelection = context.actorSelection(RootActorPath(address) / "user" / Exchange.name)
}