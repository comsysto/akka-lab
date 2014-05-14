package com.comsysto.trading.akka

import akka.actor._
import com.comsysto.trading.domain.{Bid, Order, Security}
import com.comsysto.trading.algorithm.{AverageMarketPriceCalculator, SimpleTradeMatcher}
import akka.routing._
import akka.dispatch.Dispatchers
import com.comsysto.trading.provider.SecuritiesProvider
import akka.routing.Router
import akka.routing.Broadcast
import scala.collection.immutable.IndexedSeq
import akka.actor.Actor.Receive
import com.comsysto.trading.akka.OrderRoutingActor.{UpdateConfigResponse, UpdateConfig}

object OrderRoutingActor {
  case class UpdateConfig(config : Map[Security, ActorRef])
  case object UpdateConfigResponse
}

class OrderRoutingActor(securities : Seq[Security]) extends Actor with ActorLogging {

  val routingLogic = new OrderBookRoutingLogic()

  val router = {
    val orderBooksForSecurities = securities.foreach { security =>

      val orderBookForSecurity = context.actorOf(Props[OrderBook](
        new OrderBook(security) with SimpleTradeMatcher with AverageMarketPriceCalculator), security.name)
       routingLogic.addOrderBook(security, orderBookForSecurity)
    }
    new Router(routingLogic)
  }


  override def preStart(): Unit = {
    log.info(s"Orderbook router for $securities is starting.")
  }

  override def receive: Receive = {
    case UpdateConfig(config) => {
      routingLogic.clearRemotes()
      config.foreach {
        case (s, a) => routingLogic.addRemoteOrderBook(s, a)
      }
      sender ! UpdateConfigResponse
    }
    case msg => router.route(msg, sender())
  }
}



class OrderBookRoutingLogic extends RoutingLogic {

  private val localOrderBooks = scala.collection.mutable.Map.empty[Security, ActorRef]
  private val remoteOrderBooks = scala.collection.mutable.Map.empty[Security, ActorRef]

  private val reverseLocalOrderBooks = scala.collection.mutable.Map.empty[ActorRef, Security]

  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
    message match {
      case msg: Order => new ActorRefRoutee(select(msg.security))
        //TODO: Do we need to forward to remotes?
      case _ => new SeveralRoutees(localOrderBooks.map( e => new ActorRefRoutee(e._2)).toIndexedSeq)
    }
  }

  private def select(security : Security) : ActorRef = {
    if (localOrderBooks.contains(security)) {
      localOrderBooks(security)
    } else {
      remoteOrderBooks(security)
    }
  }

  def clearRemotes() {
    remoteOrderBooks.clear()
  }

  def addRemoteOrderBook(s: Security, a: ActorRef) ={
    remoteOrderBooks.put(s, a)
  }

  def addOrderBook(s: Security, a: ActorRef) ={
    localOrderBooks.put(s, a)
    reverseLocalOrderBooks.put(a, s)
  }

  def removeOrderBook(s: Security) : Option[ActorRef] = {
    val key = localOrderBooks.remove(s)
    key map {
      reverseLocalOrderBooks remove _
    }

    key
  }

  def removeOrderBook(a: ActorRef) : Option[Security] = {
    val key = reverseLocalOrderBooks.remove(a)
    key map {
      localOrderBooks remove _
    }

    key
  }

}
