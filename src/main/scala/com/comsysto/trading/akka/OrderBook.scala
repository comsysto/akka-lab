package com.comsysto.trading.akka

import akka.actor.{Actor, ActorLogging}
import com.comsysto.trading.domain._
import com.comsysto.trading.algorithm.TradeMatcher

object OrderBook {
  case object Trade
  case object ListPrice
  case class ListPriceResponse(currentPrice : BigDecimal)
}

class OrderBook(val security: Security, var currentPrice: BigDecimal = 0) extends Actor with TradeMatcher with ActorLogging {

  this: TradeMatcher =>

  import com.comsysto.trading.akka.OrderBook._

  //TODO: We have to store senders. As soon as an order is fulfilled we need to notify both parties so they update their balances accordingly
  var asks: List[Ask] = Nil
  var bids: List[Bid] = Nil

  override def receive = {
    case ask@Ask(s, v, p) if s == security => {
      (ask :: asks).sortWith(_.price < _.price)
      recalculate()
    }
    case bid@Bid(s, v, p) if s == security => {
      (bid :: bids).sortWith(_.price > _.price)
      recalculate()
    }
    //What to do here?
    case Trade =>

    case ListPrice => sender ! ListPriceResponse(currentPrice)
  }

  private def recalculate() {
    val (newAsks, newBids, newPrice) = doTrades(asks, bids)
    asks = newAsks
    bids = newBids
    currentPrice = newPrice
  }

  def doTrades(asks: List[Ask], bids: List[Bid]) : (List[Ask], List[Bid], BigDecimal) = {
    if (!bids.isEmpty && !asks.isEmpty) {
      val topOfBook = (bids.head, asks.head)
      //TODO: What to return in each case?
      topOfBook match {
        case (bid, ask) if bid.price < ask.price ⇒ (asks, bids, currentPrice) // no match
        case (bid, ask) if bid.price >= ask.price && bid.volume == ask.volume ⇒
          //trade(bid, ask)
          doTrades(asks.tail, bids.tail)
        case (bid, ask) if bid.price >= ask.price && bid.volume < ask.volume ⇒
          val matchingAsk = ask.split(bid.volume)
          val remainingAsk = ask.split(ask.volume - bid.volume)
          //trade(bid, matchingAsk)
          doTrades(remainingAsk :: asks.tail, bids.tail)
        case (bid, ask) if bid.price >= ask.price && bid.volume > ask.volume ⇒
          val matchingBid = bid.split(ask.volume)
          val remainingBid = bid.split(bid.volume - ask.volume)
          //trade(matchingBid, ask)
          doTrades(asks.tail, remainingBid :: bids.tail)
      }
    } else {
      //No change - price stays as is
      (asks, bids, currentPrice)
    }
  }

}
