package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.{Bid, Ask}

private[algorithm] case class SuccessfulTrade(volume: Long, price: BigDecimal)

trait SimpleTradeMatcher extends TradeMatcher {


  override def doTrades(asks: List[Ask], bids: List[Bid], lastPrice : BigDecimal) : (List[Ask], List[Bid], BigDecimal) = {
    val sortedAsks = asks.sortWith(_.price < _.price)
    val sortedBids = bids.sortWith(_.price > _.price)

    val (remainingAsks, remainingBids, successfulTrades) = doMatch(sortedAsks, sortedBids, List[SuccessfulTrade]())
    (remainingAsks, remainingBids, calculatePrice(successfulTrades, lastPrice))
  }

  private[algorithm] def doMatch(asks: List[Ask], bids: List[Bid], successfulTrades : List[SuccessfulTrade]) : (List[Ask], List[Bid], List[SuccessfulTrade]) = {
    if (!bids.isEmpty && !asks.isEmpty) {
      val topOfBook = (bids.head, asks.head)
      topOfBook match {
        case (bid, ask) if bid.price < ask.price ⇒ (asks, bids, successfulTrades) // no match
        case (bid, ask) if bid.price >= ask.price && bid.volume == ask.volume ⇒
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

  private[algorithm] def calculatePrice(successfulTrades : List[SuccessfulTrade], lastPrice : BigDecimal) : BigDecimal = {
    successfulTrades match {
      case Nil => lastPrice
      case _ => {
        val components = successfulTrades.foldLeft[(BigDecimal, Long)]((0, 0)){ (res, trade) =>
          (res._1 + (trade.price * trade.volume), res._2 + trade.volume)
        }
        components._1 / components._2
      }
    }
  }

}
