package com.comsysto.trading.akka

import akka.actor._
import com.comsysto.trading.domain.Order
import com.comsysto.trading.algorithm.{AverageMarketPriceCalculator, SimpleTradeMatcher}
import akka.routing._
import akka.dispatch.Dispatchers
import com.comsysto.trading.provider.SecuritiesProvider
import com.comsysto.trading.domain.Security
import akka.routing.Router
import akka.routing.Broadcast
import scala.collection.immutable.IndexedSeq

object OrderRouter {


  case object ListSecurities
  case class ListSecuritiesResponse(securities : List[Security])

}

/**
 * Created by sturmm on 11.03.14.
 */
class OrderRouter extends Pool {
  this: SecuritiesProvider =>

  override def supervisorStrategy = SupervisorStrategy.defaultStrategy

  override def routerDispatcher = Dispatchers.DefaultDispatcherId

  override def createRouter(system: ActorSystem): Router = {
    val orderBooksForSecurities = securities.map { security =>

        val orderBookForSecurity = system.actorOf(Props[OrderBook](
          new OrderBook(security) with SimpleTradeMatcher with AverageMarketPriceCalculator))

      security -> orderBookForSecurity
    }
    new Router(new OrderBookRoutingLogic(orderBooksForSecurities.toMap))
  }

  override def resizer: Option[Resizer] = None

  override def nrOfInstances: Int = securities.size
}

class OrderBookRoutingLogic(orderBooks: Map[Security, ActorRef]) extends RoutingLogic {

  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
    message match {
      case msg: Order => new ActorRefRoutee(orderBooks(msg.security))
      case msg: Broadcast => new SeveralRoutees(orderBooks.map( e => new ActorRefRoutee(e._2)).toIndexedSeq)
    }
  }
}
