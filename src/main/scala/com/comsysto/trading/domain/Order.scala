package com.comsysto.trading.domain

import java.util.Date

sealed trait Order {
  val security: Security
  val volume: Long
  val price: BigDecimal
  //TODO: Better use System.nanoTime (typically higher resolution)?
  val requested = new Date()
}

case class Bid(security: Security, volume: Long, price: BigDecimal) extends Order {
  def split(newVolume: Long) = {
    new Bid(security, newVolume, price)
  }
}
case class Ask(security: Security, volume: Long, price: BigDecimal) extends Order {
  def split(newVolume : Long) = {
    new Ask(security, newVolume, price)
  }
}




