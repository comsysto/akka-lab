package com.comsysto.trading.algorithm

import org.scalatest._
import com.comsysto.trading.domain._
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Bid
import com.comsysto.trading.domain.Ask
import com.comsysto.trading.domain.SuccessfulTrade

class SimpleTradeMatcherSpec extends WordSpecLike with Matchers {
  val daimler = new Security("DE0007100000")

  val bidder = new Depot("0815", daimler, 100000)
  val asker = new Depot("4711", daimler, 100000)

  val tradeMatcher = new SimpleTradeMatcher {}

  "SimpleTradeMatcher" should {
    "should trade same price" in {
      val bid = new Bid(bidder, daimler, 1000, 100)
      val ask = new Ask(asker, daimler, 1000, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be('empty)
      remainingBids should be('empty)
      successfulTrades should equal(new SuccessfulTrade(bid, ask, 1000, 100) :: Nil)
    }

    "should trade two levels" in {
      val bid1 = new Bid(bidder, daimler, 1000, 101)
      val bid2 = new Bid(bidder, daimler, 1000, 100)
      val bid3 = new Bid(bidder, daimler, 1000, 99)

      val ask1 = new Ask(asker, daimler, 1000, 99)
      val ask2 = new Ask(asker, daimler, 1000, 100)
      val ask3 = new Ask(asker, daimler, 1000, 101)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask1 :: ask2 :: ask3 :: Nil, bid1 :: bid2 :: bid3 :: Nil, Nil)

      remainingAsks should be(ask3 :: Nil)
      remainingBids should be(bid3 :: Nil)
      successfulTrades should equal(new SuccessfulTrade(bid2, ask2, 1000, 100) :: new SuccessfulTrade(bid1, ask1, 1000, 101) :: Nil)
    }

    "should split bid" in {
      val bid = new Bid(bidder, daimler, 300, 100)
      val ask = new Ask(asker, daimler, 1000, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be(new Ask(asker, daimler, 700, 100) :: Nil)
      remainingBids should be('empty)
      successfulTrades should equal(new SuccessfulTrade(bid, ask, 300, 100) :: Nil)
    }

    "should split ask" in {
      val bid = new Bid(bidder, daimler, 1000, 100)
      val ask = new Ask(asker, daimler, 600, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be('empty)
      remainingBids should be(new Bid(bidder, daimler, 400, 100) :: Nil)
      successfulTrades should equal(new SuccessfulTrade(bid, ask, 600, 100) :: Nil)
    }
  }
}
