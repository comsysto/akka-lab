package com.comsysto.trading.akka

import akka.actor.{ActorContext, SupervisorStrategy, Props, ActorRef}
import com.comsysto.trading.domain.Order
import com.comsysto.trading.algorithm.{AverageMarketPriceCalculator, SimpleTradeMatcher}
import akka.routing._
import akka.dispatch.Dispatchers
import com.comsysto.trading.domain.Security
import akka.routing.Destination

object OrderRouter {

  case object ListSecurities
  case class ListSecuritiesResponse(securities : List[Security])

  def apply(securities : List[Security]) = new OrderRouter(securities)
}

/**
 * Created by sturmm on 11.03.14.
 */
class OrderRouter(val securities : List[Security]) extends RouterConfig {
  private val orderBooks = scala.collection.mutable.Map.empty[Security, ActorRef]

  override def supervisorStrategy = SupervisorStrategy.defaultStrategy

  override def routerDispatcher = Dispatchers.DefaultDispatcherId


  override def createRoute(routeeProvider: RouteeProvider) : Route = {
    val orderBooksForSecurities = securities.map { security =>
      val orderBookForSecurity = routeeProvider.context.actorOf(Props[OrderBook](
        new OrderBook(security) with SimpleTradeMatcher with AverageMarketPriceCalculator))
      orderBooks(security) = orderBookForSecurity
      orderBookForSecurity
    }

    routeeProvider.registerRoutees(orderBooksForSecurities)

    {
      case (sender, message: Order) => List(Destination(sender, orderBooks(message.security)))
      case (sender, message: Broadcast) => orderBooksForSecurities map (Destination(sender, _))
    }
  }
}
