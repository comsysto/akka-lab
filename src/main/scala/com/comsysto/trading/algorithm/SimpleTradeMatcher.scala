package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.{SuccessfulTrade, Bid, Ask}
import akka.actor.ActorLogging

trait SimpleTradeMatcher extends TradeMatcher {

  //tradeObserver: TradeObserver =>
  //Hack to provide logging...
  //this: ActorLogging =>

  override def doTrades(asks: List[Ask], bids: List[Bid]) : (List[Ask], List[Bid], List[SuccessfulTrade]) = {
    val sortedAsks = asks.sortWith(_.price < _.price)
    val sortedBids = bids.sortWith(_.price > _.price)
    //log.info(s"Successful trades: $successfulTrades")
    doMatch(sortedAsks, sortedBids, Nil)
  }

  private[algorithm] def doMatch(asks: List[Ask], bids: List[Bid], successfulTrades : List[SuccessfulTrade]) : (List[Ask], List[Bid], List[SuccessfulTrade]) = {
    if (!bids.isEmpty && !asks.isEmpty) {
      val topOfBook = (bids.head, asks.head)
      topOfBook match {
        case (bid, ask) if bid.price < ask.price ⇒ (asks, bids, successfulTrades) // no match
        case (bid, ask) if bid.price >= ask.price && bid.volume == ask.volume ⇒
          //trade(bid, ask)
          doMatch(asks.tail, bids.tail, new SuccessfulTrade(bid.volume, bid.price) :: successfulTrades)
        case (bid, ask) if bid.price >= ask.price && bid.volume < ask.volume ⇒
          val matchingAsk = ask.split(bid.volume)
          val remainingAsk = ask.split(ask.volume - bid.volume)
          //trade(bid, matchingAsk)
          doMatch(remainingAsk :: asks.tail, bids.tail, new SuccessfulTrade(bid.volume, bid.price) :: successfulTrades)
        case (bid, ask) if bid.price >= ask.price && bid.volume > ask.volume ⇒
          val matchingBid = bid.split(ask.volume)
          val remainingBid = bid.split(bid.volume - ask.volume)
          //trade(matchingBid, ask)
          doMatch(asks.tail, remainingBid :: bids.tail, new SuccessfulTrade(matchingBid.volume, matchingBid.price) :: successfulTrades)
      }
    } else {
      //No change - price stays as is
      (asks, bids, successfulTrades)
    }
  }
}
