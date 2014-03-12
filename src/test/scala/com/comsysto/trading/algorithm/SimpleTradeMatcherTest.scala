package com.comsysto.trading.algorithm

import org.scalatest._
import com.comsysto.trading.domain.{Security, Ask, Bid}

class SimpleTradeMatcherTest extends WordSpecLike with Matchers {
  val daimler = new Security("DE0007100000")

  val tradeMatcher = new SimpleTradeMatcher {}


  "SimpleTradeMatcher" should {
    "leave price as is if no trades match" in {
      tradeMatcher.calculatePrice(Nil, 5) should be(5)
    }

    "calculate average of trades" in {
      tradeMatcher.calculatePrice(new SuccessfulTrade(10, 20) :: new SuccessfulTrade(10, 10) :: Nil, 5) should be(15)
    }

    "should trade same price" in {
      val bid = new Bid(daimler, 1000, 100)
      val ask = new Ask(daimler, 1000, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be('empty)
      remainingBids should be('empty)
      successfulTrades should equal(new SuccessfulTrade(1000, 100) :: Nil)
    }

    "should trade two levels" in {
      val bid1 = new Bid(daimler, 1000, 101)
      val bid2 = new Bid(daimler, 1000, 100)
      val bid3 = new Bid(daimler, 1000, 99)

      val ask1 = new Ask(daimler, 1000, 99)
      val ask2 = new Ask(daimler, 1000, 100)
      val ask3 = new Ask(daimler, 1000, 101)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask1 :: ask2 :: ask3 :: Nil, bid1 :: bid2 :: bid3 :: Nil, Nil)

      remainingAsks should be(ask3 :: Nil)
      remainingBids should be(bid3 :: Nil)
      successfulTrades should equal(new SuccessfulTrade(1000, 100) :: new SuccessfulTrade(1000, 101) :: Nil)
    }

    "should split bid" in {
      val bid = new Bid(daimler, 300, 100)
      val ask = new Ask(daimler, 1000, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be(new Ask(daimler, 700, 100) :: Nil)
      remainingBids should be('empty)
      successfulTrades should equal(new SuccessfulTrade(300, 100) :: Nil)
    }

    "should split ask" in {
      val bid = new Bid(daimler, 1000, 100)
      val ask = new Ask(daimler, 600, 100)

      val (remainingAsks, remainingBids, successfulTrades) = tradeMatcher.doMatch(ask :: Nil, bid :: Nil, Nil)

      remainingAsks should be('empty)
      remainingBids should be(new Bid(daimler, 400, 100) :: Nil)
      successfulTrades should equal(new SuccessfulTrade(600, 100) :: Nil)
    }
  }
}
