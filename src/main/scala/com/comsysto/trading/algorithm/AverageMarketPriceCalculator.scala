package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.SuccessfulTrade

trait AverageMarketPriceCalculator extends MarketPriceCalculator {
  override def calculatePrice(successfulTrades : List[SuccessfulTrade], lastPrice : BigDecimal) : BigDecimal = {
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
