package com.comsysto.trading.akka

import akka.actor.{ActorRef, Props, Actor}
import com.comsysto.trading.akka.Exchange._
import com.comsysto.trading.provider.{SecuritiesProvider, SimpleSecuritiesProvider}
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Security
import com.comsysto.trading.akka.Exchange.Open
import com.comsysto.trading.akka.Exchange.ListSecuritiesResponse
import com.comsysto.trading.akka.OrderRoutingActor.{UpdateConfigResponse, UpdateConfig}

object Exchange {
  val name = "exchange"

  case object ListSecurities
  case class ListSecuritiesResponse(securities : Map[Security, ActorRef])

  case class Open(globalConfig : Map[Security, ActorRef])
  case object OpenedResponse
  case object Close
}

class Exchange(val securities : Seq[Security]) extends Actor {
  private val orderRouter = context.actorOf(Props[OrderRoutingActor](new OrderRoutingActor(securities)), "orderbooks")

  def receive = closedMarket

  //TODO: Provide securities
  def closedMarket : Receive = {
    case ListSecurities => sender ! ListSecuritiesResponse(securities.map(_ -> orderRouter).toMap)

    case o@Open(globalConfig) => {
      orderRouter ! UpdateConfig(globalConfig)
      context.become(configuringRouter(sender(), o))
    }
  }

  def configuringRouter(origin: ActorRef, msg: Open) : Receive = {
    case UpdateConfigResponse => {
      origin ! OpenedResponse
      context.become(openMarket)
    }
  }

  def openMarket : Receive = {
    //TODO: Shutdown any dependent actors?
    case Close => context.become(closedMarket)
  }
}
