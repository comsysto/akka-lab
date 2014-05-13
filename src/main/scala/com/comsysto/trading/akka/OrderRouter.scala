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
import akka.actor.Actor.Receive

object OrderRouterActor {


  case object ListSecurities
  case class ListSecuritiesResponse(securities : List[Security])

}

class OrderRoutingActor extends Actor with ActorLogging{
  this: SecuritiesProvider =>

  val routingLogic = new OrderBookRoutingLogic()

  val router = {


    val orderBooksForSecurities = securities.foreach { security =>

      val orderBookForSecurity = context.actorOf(Props[OrderBook](
        new OrderBook(security) with SimpleTradeMatcher with AverageMarketPriceCalculator), security.name)

       routingLogic.addOrderBook(security, orderBookForSecurity)
    }
    new Router(routingLogic)
  }


  override def receive: Receive = {
    case Terminated(a) => {
      routingLogic.removeOrderBook(a) map {
        s =>
          val r = context.actorOf(Props[OrderBook](
            new OrderBook(s) with SimpleTradeMatcher with AverageMarketPriceCalculator), s.name)
          context watch r
          routingLogic.addOrderBook(s, r)
      }
    }
    case msg => {
      router.route(msg, sender)
    }
  }
}



class OrderBookRoutingLogic extends RoutingLogic {

  private val orderBooks = scala.collection.mutable.Map.empty[Security, ActorRef]
  private val reverseOrderBooks = scala.collection.mutable.Map.empty[ActorRef, Security]

  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
    message match {
      case msg: Order => new ActorRefRoutee(orderBooks(msg.security))
      case msg: Broadcast => new SeveralRoutees(orderBooks.map( e => new ActorRefRoutee(e._2)).toIndexedSeq)
    }
  }

  def addOrderBook(s: Security, a: ActorRef) ={
    orderBooks.put(s, a)
    reverseOrderBooks.put(a, s)
  }

  def removeOrderBook(s: Security) : Option[ActorRef] = {
    val key = orderBooks.remove(s)
    key map {
      reverseOrderBooks remove _
    }

    key
  }

  def removeOrderBook(a: ActorRef) : Option[Security] = {
    val key = reverseOrderBooks.remove(a)
    key map {
      orderBooks remove _
    }

    key
  }

}
