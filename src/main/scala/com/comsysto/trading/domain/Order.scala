package com.comsysto.trading.domain

import java.util.Date

//List(SuccessfulTrade(
//  Bid(Depot(0815,Security(DE0007100000),100000),Security(DE0007100000),300,100),
//  Ask(Depot(4711,Security(DE0007100000),100000),Security(DE0007100000),300,100),300,100))
//
//List(SuccessfulTrade(
//  Bid(Depot(0815,Security(DE0007100000),100000),Security(DE0007100000),300,100),
//  Ask(Depot(4711,Security(DE0007100000),100000),Security(DE0007100000),700,100),300,100))


sealed trait Order {
  val security: Security
  val volume: Long
  val price: BigDecimal
  val depot : Depot
  //TODO: Better use System.nanoTime (typically higher resolution)?
  val requested = new Date()
}

case class Bid(depot : Depot, security: Security, volume: Long, price: BigDecimal) extends Order {
  def split(newVolume: Long) = copy(volume = newVolume)
}


case class Ask(depot : Depot, security: Security, volume: Long, price: BigDecimal) extends Order {
  def split(newVolume : Long) = copy(volume = newVolume)
}

case class SuccessfulTrade(bid : Bid, ask : Ask, volume: Long, price: BigDecimal)



