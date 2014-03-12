package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.SuccessfulTrade

trait MarketPriceCalculator {
  def calculatePrice(successfulTrades : List[SuccessfulTrade], lastPrice : BigDecimal) : BigDecimal
}
