package com.comsysto.trading.algorithm

import com.comsysto.trading.domain.{Ask, Bid}

trait TradeObserver {
  def trade(bid : Bid, ask : Ask)
}
