package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.SuccessfulTrade
import org.scalatest.{Matchers, WordSpecLike}

class AverageMarketPriceCalculatorTest extends WordSpecLike with Matchers {
  val calculator = new AverageMarketPriceCalculator {}

  "SimpleTradeMatcher" should {
    "leave price as is if no trades match" in {
      calculator.calculatePrice(Nil, 5) should be(5)
    }

    "calculate average of trades" in {
      calculator.calculatePrice(new SuccessfulTrade(10, 20) :: new SuccessfulTrade(10, 10) :: Nil, 5) should be(15)
    }
  }
}
