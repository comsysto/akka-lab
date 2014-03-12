package com.comsysto.trading.algorithm

import com.comsysto.trading.domain._

trait TradeMatcher {
  def doTrades(asks: List[Ask], bids: List[Bid], lastPrice : BigDecimal) : (List[Ask], List[Bid], BigDecimal)

}
