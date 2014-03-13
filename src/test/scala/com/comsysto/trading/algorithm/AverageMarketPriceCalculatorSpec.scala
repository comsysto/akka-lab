package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.{Ask, Bid, SuccessfulTrade}
import org.scalatest.{Matchers, WordSpecLike}

class AverageMarketPriceCalculatorSpec extends WordSpecLike with Matchers {
  val anyBid : Bid = null
  val anyAsk : Ask = null

  val calculator = new AverageMarketPriceCalculator {}



  "SimpleTradeMatcher" should {
    "leave price as is if no trades match" in {
      calculator.calculatePrice(Nil, 5) should be(5)
    }

    "calculate average of trades" in {
      calculator.calculatePrice(new SuccessfulTrade(anyBid, anyAsk, 10, 20) :: new SuccessfulTrade(anyBid, anyAsk, 10, 10) :: Nil, 5) should be(15)
    }
  }
}
