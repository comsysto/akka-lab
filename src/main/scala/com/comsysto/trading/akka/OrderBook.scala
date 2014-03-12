package com.comsysto.trading.akka

import akka.actor.{Actor, ActorLogging}
import com.comsysto.trading.domain._
import com.comsysto.trading.algorithm.TradeMatcher

object OrderBook {
  //TODO: Implement scheduling
  case object Trade
  case object ListPrice
  case class ListPriceResponse(currentPrice : BigDecimal)
}

class OrderBook(val security: Security, var currentPrice: BigDecimal = 0) extends Actor with ActorLogging {

  this: TradeMatcher =>

  import com.comsysto.trading.akka.OrderBook._

  //TODO: We have to store senders. As soon as an order is fulfilled we need to notify both parties so they update their balances accordingly
  var asks: List[Ask] = Nil
  var bids: List[Bid] = Nil

  override def receive = {
    case ask@Ask(s, v, p) if s == security => asks = ask :: asks
    case bid@Bid(s, v, p) if s == security => bids = bid :: bids
    case Trade => recalculate()

    case ListPrice => sender ! ListPriceResponse(currentPrice)
  }

  private def recalculate() {
    val (newAsks, newBids, newPrice) = doTrades(asks, bids, currentPrice)
    asks = newAsks
    bids = newBids
    currentPrice = newPrice
  }
}
